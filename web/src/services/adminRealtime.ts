import type { AdminRealtimeEvent } from '@/types'

type OrderEventListener = (event: AdminRealtimeEvent) => void
type ResetListener = () => void

interface StartOptions {
  token: string
  cursor: number
  onUnauthorized: () => void
}

interface ParsedSseEvent {
  event: string
  id?: string
  data: string
  retry?: number
}

class AdminRealtimeClient {
  private orderListeners = new Set<OrderEventListener>()
  private resetListeners = new Set<ResetListener>()
  private abortController: AbortController | null = null
  private runId = 0
  private lastEventId = 0
  private retryMillis = 1000

  start(options: StartOptions) {
    this.stop()
    this.lastEventId = options.cursor
    const currentRun = ++this.runId
    void this.connectLoop(currentRun, options)
  }

  stop() {
    this.runId++
    this.abortController?.abort()
    this.abortController = null
  }

  subscribeOrders(listener: OrderEventListener) {
    this.orderListeners.add(listener)
    return () => this.orderListeners.delete(listener)
  }

  subscribeReset(listener: ResetListener) {
    this.resetListeners.add(listener)
    return () => this.resetListeners.delete(listener)
  }

  private async connectLoop(runId: number, options: StartOptions) {
    let failures = 0
    while (runId === this.runId) {
      try {
        const result = await this.connectOnce(runId, options)
        if (result === 'unauthorized') {
          options.onUnauthorized()
          return
        }
        failures = 0
      } catch (error) {
        if (runId !== this.runId || (error instanceof DOMException && error.name === 'AbortError')) {
          return
        }
        failures++
      }

      const backoff = Math.min(this.retryMillis * Math.max(1, 2 ** failures), 15000)
      await new Promise(resolve => window.setTimeout(resolve, backoff))
    }
  }

  private async connectOnce(runId: number, options: StartOptions) {
    const controller = new AbortController()
    this.abortController = controller
    const headers: Record<string, string> = {
      Accept: 'text/event-stream',
      Authorization: `Bearer ${options.token}`,
      'Cache-Control': 'no-cache',
    }
    headers['Last-Event-ID'] = String(this.lastEventId)

    const response = await fetch('/api/v1/admin/realtime/stream', {
      method: 'GET',
      headers,
      cache: 'no-store',
      signal: controller.signal,
    })
    if (response.status === 401 || response.status === 403) {
      return 'unauthorized' as const
    }
    if (!response.ok || !response.body) {
      throw new Error(`SSE connection failed: ${response.status}`)
    }

    this.retryMillis = 1000
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (runId === this.runId) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      buffer = this.consumeBuffer(buffer)
    }
    buffer += decoder.decode()
    this.consumeBuffer(buffer, true)
    return 'closed' as const
  }

  private consumeBuffer(input: string, flush = false) {
    let buffer = input
    while (true) {
      const delimiter = /\r\n\r\n|\n\n|\r\r/.exec(buffer)
      if (!delimiter) break
      const block = buffer.slice(0, delimiter.index)
      buffer = buffer.slice(delimiter.index + delimiter[0].length)
      this.handleBlock(block)
    }
    if (flush && buffer.trim()) {
      this.handleBlock(buffer)
      return ''
    }
    return buffer
  }

  private handleBlock(block: string) {
    const parsed = this.parseBlock(block)
    if (!parsed) return
    if (parsed.retry !== undefined) {
      this.retryMillis = Math.min(Math.max(parsed.retry, 500), 15000)
    }

    const numericId = parsed.id ? Number(parsed.id) : undefined
    if (parsed.event === 'order.event') {
      const event = JSON.parse(parsed.data) as AdminRealtimeEvent
      if (!Number.isFinite(event.eventId) || event.eventId <= this.lastEventId) return
      this.lastEventId = event.eventId
      for (const listener of this.orderListeners) {
        try { listener(event) } catch { /* 单个页面监听器不能中断实时连接 */ }
      }
      return
    }

    if (parsed.event === 'realtime.reset') {
      if (numericId !== undefined && Number.isFinite(numericId)) {
        this.lastEventId = numericId
      }
      for (const listener of this.resetListeners) {
        try { listener() } catch { /* 单个页面监听器不能中断实时连接 */ }
      }
    }
  }

  private parseBlock(block: string): ParsedSseEvent | null {
    let event = 'message'
    let id: string | undefined
    let retry: number | undefined
    const data: string[] = []

    for (const line of block.split(/\r\n|\n|\r/)) {
      if (!line || line.startsWith(':')) continue
      const separator = line.indexOf(':')
      const field = separator < 0 ? line : line.slice(0, separator)
      let value = separator < 0 ? '' : line.slice(separator + 1)
      if (value.startsWith(' ')) value = value.slice(1)
      if (field === 'event') event = value
      else if (field === 'id') id = value
      else if (field === 'data') data.push(value)
      else if (field === 'retry' && /^\d+$/.test(value)) retry = Number(value)
    }
    if (data.length === 0 && retry === undefined) return null
    return { event, id, data: data.join('\n'), retry }
  }
}

export const adminRealtime = new AdminRealtimeClient()
