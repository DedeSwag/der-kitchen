export interface MealRequest {
  mealType: 'breakfast' | 'lunch' | 'dinner'
  mealDate: string
}

function formatLocalDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function dateAfter(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return formatLocalDate(date)
}

export function toMealRequest(selection: string): MealRequest {
  const match = /^(today|tomorrow)_(breakfast|lunch|dinner)$/.exec(selection)
  if (!match) {
    throw new Error('用餐时段不合法')
  }
  return {
    mealType: match[2] as MealRequest['mealType'],
    mealDate: dateAfter(match[1] === 'tomorrow' ? 1 : 0),
  }
}

export function formatMealLabel(mealDate: string, mealType: string): string {
  const mealNames: Record<string, string> = {
    breakfast: '早餐',
    lunch: '午餐',
    dinner: '晚餐',
  }
  let dateLabel = mealDate
  if (mealDate === dateAfter(0)) {
    dateLabel = '今日'
  } else if (mealDate === dateAfter(1)) {
    dateLabel = '明日'
  } else {
    const parts = mealDate.split('-')
    if (parts.length === 3) {
      dateLabel = `${Number(parts[1])}月${Number(parts[2])}日`
    }
  }
  return `${dateLabel}${mealNames[mealType] || mealType}`
}

export function today(): string {
  return dateAfter(0)
}
