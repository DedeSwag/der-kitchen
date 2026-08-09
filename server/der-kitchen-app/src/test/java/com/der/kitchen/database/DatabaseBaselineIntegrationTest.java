package com.der.kitchen.database;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.der.kitchen.category.entity.Category;
import com.der.kitchen.category.mapper.CategoryMapper;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.dish.mapper.DishMapper;
import com.der.kitchen.favorite.entity.Favorite;
import com.der.kitchen.favorite.mapper.FavoriteMapper;
import com.der.kitchen.file.entity.SysFile;
import com.der.kitchen.file.mapper.SysFileMapper;
import com.der.kitchen.order.entity.Order;
import com.der.kitchen.order.entity.OrderItem;
import com.der.kitchen.order.mapper.OrderItemMapper;
import com.der.kitchen.order.mapper.OrderMapper;
import com.der.kitchen.notification.entity.Notification;
import com.der.kitchen.notification.entity.OrderEvent;
import com.der.kitchen.notification.mapper.NotificationMapper;
import com.der.kitchen.notification.mapper.OrderEventMapper;
import com.der.kitchen.notification.service.WechatDelivery;
import com.der.kitchen.notification.service.WechatDeliveryStore;
import com.der.kitchen.stats.service.StatsService;
import com.der.kitchen.stats.vo.DailySummaryVO;
import com.der.kitchen.stats.vo.StatsOverviewVO;
import com.der.kitchen.stats.vo.TopDishVO;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.LocalDate;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class DatabaseBaselineIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("der_kitchen")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>("minio/minio:latest")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server", "/data")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("minio.endpoint", () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
        registry.add("minio.bucket", () -> "kitchen-test");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private SysFileMapper sysFileMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderEventMapper orderEventMapper;
    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private WechatDeliveryStore wechatDeliveryStore;
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private StatsService statsService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    void migratesEmptyDatabaseAndSupportsCoreMapperWrites() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                """, String.class);
        assertThat(tables).contains(
                "sys_user", "sys_file", "dish_category", "dish_info",
                "biz_order", "biz_order_item", "biz_order_event", "sys_notification",
                "biz_favorite", "flyway_schema_history");
        List<String> dishColumns = jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'dish_info'
                """, String.class);
        assertThat(dishColumns).contains("image_file_id").doesNotContain("image_url", "file_key");

        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "admin"));
        assertThat(user).isNotNull();

        Category category = new Category();
        category.setName("测试分类");
        category.setSortOrder(99);
        category.setStatus("active");
        categoryMapper.insert(category);
        assertThat(category.getId()).isPositive();

        SysFile file = new SysFile();
        file.setOriginalName("dish.webp");
        file.setFileKey("test/dish.webp");
        file.setBucket("kitchen");
        file.setSizeBytes(128L);
        file.setMimeType("image/webp");
        file.setUploadBy(user.getId());
        sysFileMapper.insert(file);
        assertThat(file.getId()).isPositive();

        Dish dish = new Dish();
        dish.setName("测试菜品");
        dish.setImageFileId(file.getId());
        dish.setCategoryId(category.getId());
        dish.setCookingTime(10);
        dish.setStatus("normal");
        dish.setIsListed(true);
        dishMapper.insert(dish);
        assertThat(dish.getId()).isPositive();

        Order order = new Order();
        order.setUserId(user.getId());
        order.setMealType("dinner");
        order.setMealDate(LocalDate.now());
        order.setStatus("pending");
        order.setTasteTags(List.of("微辣", "少油"));
        orderMapper.insert(order);
        assertThat(orderMapper.selectById(order.getId()).getTasteTags())
                .containsExactly("微辣", "少油");

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setDishId(dish.getId());
        orderItem.setDishName(dish.getName());
        orderItem.setQuantity(1);
        orderItem.setIsExtra(false);
        orderItemMapper.insert(orderItem);
        assertThat(orderItem.getId()).isPositive();

        Favorite favorite = new Favorite();
        favorite.setUserId(user.getId());
        favorite.setDishId(dish.getId());
        favoriteMapper.insert(favorite);
        favoriteMapper.deleteById(favorite.getId());

        Favorite restored = new Favorite();
        restored.setUserId(user.getId());
        restored.setDishId(dish.getId());
        assertThat(favoriteMapper.insert(restored)).isEqualTo(1);

        assertThat(statsService.overview()).isNotNull();
        assertThat(statsService.topDishes(LocalDate.now(), LocalDate.now(), 10)).isNotNull();
        assertThat(statsService.dailySummary(LocalDate.now(), LocalDate.now())).isNotNull();
    }

    @Test
    @Transactional
    void enforcesAuthenticationRoleAndAccountStatus() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        User wife = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, "test_wife_openid"));
        assertThat(wife).isNotNull();
        String userToken = StpUtil.getStpLogic().createLoginSession(wife.getId());

        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/v1/auth/admin-login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String adminToken = adminLogin("Abc@1234");
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        wife.setStatus("disabled");
        userMapper.updateById(wife);
        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Transactional
    void preventsUsersFromReadingAnotherUsersOrder() throws Exception {
        User admin = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "admin"));
        User wife = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, "test_wife_openid"));

        Order adminOrder = new Order();
        adminOrder.setUserId(admin.getId());
        adminOrder.setMealType("dinner");
        adminOrder.setMealDate(LocalDate.now());
        adminOrder.setStatus("pending");
        adminOrder.setTasteTags(List.of());
        orderMapper.insert(adminOrder);

        String wifeToken = StpUtil.getStpLogic().createLoginSession(wife.getId());
        mockMvc.perform(get("/api/v1/orders/{id}", adminOrder.getId())
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/v1/admin/orders/{id}", adminOrder.getId())
                        .header("Authorization", "Bearer " + adminLogin("Abc@1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(adminOrder.getId()));
    }

    @Test
    @Transactional
    void invalidatesSessionsAfterLogoutAndPasswordChange() throws Exception {
        String logoutToken = adminLogin("Abc@1234");
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + logoutToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + logoutToken))
                .andExpect(status().isUnauthorized());

        String firstToken = adminLogin("Abc@1234");
        String secondToken = adminLogin("Abc@1234");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"Abc@1234","newPassword":"admin456"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void returnsStandardHttpStatusesForBadRequestsAndMissingResources() throws Exception {
        mockMvc.perform(post("/api/v1/auth/admin-login")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/missing-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/v1/auth/admin-login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    @Transactional
    void closesCategoryDishFileAndFavoriteLifecycle() throws Exception {
        User admin = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "admin"));
        User wife = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, "test_wife_openid"));
        String adminToken = adminLogin("Abc@1234");
        String wifeToken = StpUtil.getStpLogic().createLoginSession(wife.getId());

        Category category = new Category();
        category.setName("闭环分类");
        category.setSortOrder(80);
        category.setStatus("active");
        categoryMapper.insert(category);
        SysFile image = uploadImage(adminToken, "lifecycle.png");

        String createResponse = mockMvc.perform(post("/api/v1/admin/dishes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"闭环菜品","description":"只保存文件ID","imageFileId":%d,
                                 "categoryId":%d,"cookingTime":20}
                                """.formatted(image.getId(), category.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageFileId").value(image.getId()))
                .andExpect(jsonPath("$.data.imageUrl").isString())
                .andReturn().getResponse().getContentAsString();
        Long dishId = objectMapper.readTree(createResponse).path("data").path("id").asLong();
        assertThat(dishMapper.selectById(dishId).getImageFileId()).isEqualTo(image.getId());

        mockMvc.perform(put("/api/v1/admin/dishes/{id}/status", dishId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"out_of_stock\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/dishes/{id}", dishId)
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("out_of_stock"));
        mockMvc.perform(put("/api/v1/admin/dishes/{id}/listing", dishId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"isListed\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/dishes/{id}", dishId)
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/admin/dishes/{id}/listing", dishId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"isListed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/favorites")
                        .header("Authorization", "Bearer " + wifeToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"dishId\":" + dishId + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/favorites")
                        .header("Authorization", "Bearer " + wifeToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"dishId\":" + dishId + "}"))
                .andExpect(status().isOk());
        assertThat(favoriteMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, wife.getId())
                        .eq(Favorite::getDishId, dishId))).isEqualTo(1);
        mockMvc.perform(get("/api/v1/favorites")
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(dishId))
                .andExpect(jsonPath("$.data[0].imageFileId").value(image.getId()))
                .andExpect(jsonPath("$.data[0].imageUrl").isString());

        mockMvc.perform(delete("/api/common/file/{id}", image.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
        mockMvc.perform(delete("/api/v1/admin/categories/{id}", category.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/admin/categories/{id}", category.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"hidden\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("hidden"));
        mockMvc.perform(get("/api/v1/dishes")
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + dishId + ")]").isEmpty());
        mockMvc.perform(get("/api/v1/dishes/{id}", dishId)
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/admin/dishes/{id}", dishId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(dishId));
        mockMvc.perform(get("/api/v1/favorites")
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        Order historyOrder = new Order();
        historyOrder.setUserId(wife.getId());
        historyOrder.setMealType("dinner");
        historyOrder.setMealDate(LocalDate.now());
        historyOrder.setStatus("completed");
        historyOrder.setTasteTags(List.of());
        orderMapper.insert(historyOrder);
        OrderItem snapshot = new OrderItem();
        snapshot.setOrderId(historyOrder.getId());
        snapshot.setDishId(dishId);
        snapshot.setDishName("闭环菜品");
        snapshot.setQuantity(1);
        snapshot.setIsExtra(false);
        orderItemMapper.insert(snapshot);

        mockMvc.perform(delete("/api/v1/admin/dishes/{id}", dishId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(dishMapper.selectById(dishId)).isNull();
        assertThat(orderItemMapper.selectById(snapshot.getId())).isNotNull();
        mockMvc.perform(delete("/api/common/file/{id}", image.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/admin/categories/{id}", category.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void rejectsMissingForeignImageAndImageOwnedByAnotherUser() throws Exception {
        User admin = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "admin"));
        User wife = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, "test_wife_openid"));
        Category category = categoryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                        .eq(Category::getName, "家常菜"));
        String adminToken = adminLogin("Abc@1234");

        mockMvc.perform(post("/api/v1/admin/dishes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"没有图片\",\"categoryId\":" + category.getId() + "}"))
                .andExpect(status().isBadRequest());

        SysFile wifeImage = insertImageFile(wife, "wife.png");
        mockMvc.perform(post("/api/v1/admin/dishes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"越权图片","imageFileId":%d,"categoryId":%d}
                                """.formatted(wifeImage.getId(), category.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Transactional
    void uploadsValidatesAndDeletesImageAgainstRealMinio() throws Exception {
        String adminToken = adminLogin("Abc@1234");
        MockMultipartFile image = new MockMultipartFile(
                "files", "real.png", "text/plain", pngBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/common/file/upload")
                        .file(image)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fileId").isNumber())
                .andExpect(jsonPath("$.data[0].url").isString())
                .andReturn().getResponse().getContentAsString();
        Long fileId = objectMapper.readTree(uploadResponse).path("data").get(0).path("fileId").asLong();
        SysFile stored = sysFileMapper.selectById(fileId);
        assertThat(stored.getMimeType()).isEqualTo("image/png");
        assertThat(stored.getFileKey()).startsWith("images/");

        mockMvc.perform(delete("/api/common/file/{id}", fileId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(sysFileMapper.selectById(fileId)).isNull();

        MockMultipartFile disguised = new MockMultipartFile(
                "files", "fake.jpg", "image/jpeg", pngBytes());
        mockMvc.perform(multipart("/api/common/file/upload")
                        .file(disguised)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Transactional
    void closesOrderCreationListingExtraItemAndStatusLifecycle() throws Exception {
        User admin = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "admin"));
        User wife = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, "test_wife_openid"));
        String adminToken = adminLogin("Abc@1234");
        String wifeToken = StpUtil.getStpLogic().createLoginSession(wife.getId());

        Category category = new Category();
        category.setName("订单闭环分类");
        category.setSortOrder(81);
        category.setStatus("active");
        categoryMapper.insert(category);

        SysFile image = insertImageFile(admin, "order-lifecycle.png");
        Dish dish = new Dish();
        dish.setName("订单快照菜品");
        dish.setImageFileId(image.getId());
        dish.setCategoryId(category.getId());
        dish.setCookingTime(15);
        dish.setStatus("normal");
        dish.setIsListed(true);
        dishMapper.insert(dish);

        String createResponse = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + wifeToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"mealType":"dinner","mealDate":"%s",
                                 "items":[{"dishId":%d,"quantity":2}],
                                 "tasteTags":["微辣","少油"],
                                 "dietaryNotes":"不要香菜","specialRequests":"软烂一点"}
                                """.formatted(LocalDate.now(), dish.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.items[0].dishName").value("订单快照菜品"))
                .andExpect(jsonPath("$.data.items[0].isExtra").value(false))
                .andExpect(jsonPath("$.data.tasteTags[0]").value("微辣"))
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/orders")
                        .param("status", "ongoing")
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.id == " + orderId + ")].items[0].dishName")
                        .exists());
        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("status", "pending,preparing")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.id == " + orderId + ")].userNickname")
                        .exists());

        mockMvc.perform(put("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"cooking\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
        mockMvc.perform(put("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"preparing\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
        mockMvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Authorization", "Bearer " + wifeToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"dishId\":" + dish.getId() + ",\"quantity\":1}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"cooking\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Authorization", "Bearer " + wifeToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"dishId\":" + dish.getId() + ",\"quantity\":1}]}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"completed\"}"))
                .andExpect(status().isOk());

        dish.setName("菜品已改名");
        dishMapper.updateById(dish);
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].dishName").value("订单快照菜品"))
                .andExpect(jsonPath("$.data.items[1].isExtra").value(true));
        mockMvc.perform(get("/api/v1/orders")
                        .param("status", "history")
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.id == " + orderId + ")].status")
                        .exists());
    }

    @Test
    @Transactional
    void rejectsInvalidOrderBeforePersistingMasterAndItems() throws Exception {
        User admin = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "admin"));
        User wife = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, "test_wife_openid"));
        Category category = categoryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                        .eq(Category::getName, "家常菜"));
        SysFile image = insertImageFile(admin, "order-atomic.png");
        Dish dish = new Dish();
        dish.setName("订单原子性菜品");
        dish.setImageFileId(image.getId());
        dish.setCategoryId(category.getId());
        dish.setStatus("normal");
        dish.setIsListed(true);
        dishMapper.insert(dish);
        String wifeToken = StpUtil.getStpLogic().createLoginSession(wife.getId());
        long orderCount = orderMapper.selectCount(null);
        long itemCount = orderItemMapper.selectCount(null);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + wifeToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"mealType":"lunch","mealDate":"%s",
                                 "items":[{"dishId":%d,"quantity":1},{"dishId":999999999,"quantity":1}]}
                                """.formatted(LocalDate.now(), dish.getId())))
                .andExpect(status().isNotFound());
        assertThat(orderMapper.selectCount(null)).isEqualTo(orderCount);
        assertThat(orderItemMapper.selectCount(null)).isEqualTo(itemCount);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + wifeToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"mealType":"lunch","mealDate":"%s",
                                 "items":[{"dishId":%d,"quantity":1},{"dishId":%d,"quantity":2}]}
                                """.formatted(LocalDate.now(), dish.getId(), dish.getId())))
                .andExpect(status().isBadRequest());
        assertThat(orderMapper.selectCount(null)).isEqualTo(orderCount);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + wifeToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"mealType":"lunch","mealDate":"%s",
                                 "items":[{"dishId":%d,"quantity":1}]}
                                """.formatted(LocalDate.now().plusDays(2), dish.getId())))
                .andExpect(status().isBadRequest());
        assertThat(orderMapper.selectCount(null)).isEqualTo(orderCount);
    }

    @Test
    void serializesConcurrentTransitionsForTheSameOrder() throws Exception {
        User wife = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, "test_wife_openid"));
        Order order = new Order();
        order.setUserId(wife.getId());
        order.setMealType("dinner");
        order.setMealDate(LocalDate.now());
        order.setStatus("pending");
        order.setTasteTags(List.of());
        orderMapper.insert(order);
        String adminToken = adminLogin("Abc@1234");

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var request = (java.util.concurrent.Callable<Integer>) () -> {
                start.await();
                return mockMvc.perform(put("/api/v1/admin/orders/{id}/status", order.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(APPLICATION_JSON)
                                .content("{\"status\":\"preparing\"}"))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(request);
            Future<Integer> second = executor.submit(request);
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(orderMapper.selectById(order.getId()).getStatus()).isEqualTo("preparing");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional
    void persistsOrderEventsAndAdminNotificationsInTheOrderTransaction() throws Exception {
        User admin = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "admin"));
        User wife = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, "test_wife_openid"));
        Category category = categoryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                        .eq(Category::getName, "家常菜"));
        SysFile image = insertImageFile(admin, "notification-order.png");
        Dish dish = new Dish();
        dish.setName("通知闭环菜品");
        dish.setImageFileId(image.getId());
        dish.setCategoryId(category.getId());
        dish.setStatus("normal");
        dish.setIsListed(true);
        dishMapper.insert(dish);
        String wifeToken = StpUtil.getStpLogic().createLoginSession(wife.getId());
        String adminToken = adminLogin("Abc@1234");

        String response = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + wifeToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"mealType":"dinner","mealDate":"%s",
                                 "items":[{"dishId":%d,"quantity":1}]}
                                """.formatted(LocalDate.now(), dish.getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(response).path("data").path("id").asLong();

        assertThat(orderEventMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderEvent>()
                        .eq(OrderEvent::getOrderId, orderId)))
                .extracting(OrderEvent::getEventType)
                .containsExactly("order_created");
        Notification created = notificationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                        .eq(Notification::getOrderId, orderId)
                        .eq(Notification::getRecipientId, admin.getId())
                        .eq(Notification::getChannel, "in_app")
                        .eq(Notification::getNotificationType, "new_order"));
        assertThat(created).isNotNull();

        mockMvc.perform(get("/api/v1/admin/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.orderId == " + orderId + ")].type").exists());
        mockMvc.perform(get("/api/v1/admin/notifications")
                        .param("afterId", "0")
                        .param("limit", "20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/v1/admin/notifications")
                        .param("afterId", "-1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/api/v1/admin/notifications/{id}/read", created.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(notificationMapper.selectById(created.getId()).getReadTime()).isNotNull();

        mockMvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Authorization", "Bearer " + wifeToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"dishId\":" + dish.getId() + ",\"quantity\":1}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + wifeToken))
                .andExpect(status().isOk());

        assertThat(orderEventMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderEvent>()
                        .eq(OrderEvent::getOrderId, orderId)
                        .orderByAsc(OrderEvent::getId)))
                .extracting(OrderEvent::getEventType)
                .containsExactly("order_created", "items_added", "user_cancelled");
        assertThat(notificationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                        .eq(Notification::getOrderId, orderId)
                        .eq(Notification::getChannel, "in_app")))
                .extracting(Notification::getNotificationType)
                .containsExactlyInAnyOrder("new_order", "items_added", "order_cancelled");
    }

    @Test
    @Transactional
    void retriesWechatDeliveriesWithBoundedAttempts() {
        User admin = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "admin"));
        Order order = new Order();
        order.setUserId(admin.getId());
        order.setMealType("dinner");
        order.setMealDate(LocalDate.now());
        order.setStatus("pending");
        order.setTasteTags(List.of());
        orderMapper.insert(order);

        OrderEvent event = new OrderEvent();
        event.setOrderId(order.getId());
        event.setEventType("order_created");
        event.setActorId(admin.getId());
        event.setToStatus("pending");
        event.setItemCount(1);
        event.setDescription("重试测试事件");
        orderEventMapper.insert(event);

        Notification notification = new Notification();
        notification.setEventId(event.getId());
        notification.setOrderId(order.getId());
        notification.setRecipientId(admin.getId());
        notification.setChannel("wechat");
        notification.setNotificationType("new_order");
        notification.setTitle("重试测试");
        notification.setContent("重试测试内容");
        notification.setDeliveryStatus("pending");
        notification.setAttemptCount(0);
        notificationMapper.insert(notification);

        List<WechatDelivery> claimed = wechatDeliveryStore.claimBatch(1);
        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).attemptCount()).isEqualTo(1);
        assertThat(notificationMapper.selectById(notification.getId()).getDeliveryStatus())
                .isEqualTo("processing");

        wechatDeliveryStore.markFailed(claimed.get(0), "模拟微信异常");
        var failed = jdbcTemplate.queryForMap("""
                SELECT delivery_status, next_attempt_time, last_error
                FROM sys_notification WHERE id = ?
                """, notification.getId());
        assertThat(failed.get("delivery_status")).isEqualTo("failed");
        assertThat(failed.get("next_attempt_time")).isNotNull();
        assertThat(failed.get("last_error")).asString().contains("模拟微信异常");

        for (int expectedAttempt = 2; expectedAttempt <= 5; expectedAttempt++) {
            jdbcTemplate.update("""
                    UPDATE sys_notification
                    SET next_attempt_time = clock_timestamp() - INTERVAL '1 second'
                    WHERE id = ?
            """, notification.getId());
            List<WechatDelivery> retry = wechatDeliveryStore.claimBatch(1);
            assertThat(retry).hasSize(1);
            assertThat(retry.get(0).attemptCount()).isEqualTo(expectedAttempt);
            wechatDeliveryStore.markFailed(retry.get(0), "第" + expectedAttempt + "次失败");
        }
        var dead = jdbcTemplate.queryForMap("""
                SELECT delivery_status, next_attempt_time, attempt_count
                FROM sys_notification WHERE id = ?
                """, notification.getId());
        assertThat(dead.get("delivery_status")).isEqualTo("dead");
        assertThat(dead.get("next_attempt_time")).isNull();
        assertThat(dead.get("attempt_count")).isEqualTo(5);
        assertThat(wechatDeliveryStore.claimBatch(1)).isEmpty();
    }

    @Test
    @Transactional
    void calculatesStatisticsFromFinalTablesWithStableCancellationAndDateRules() {
        User admin = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, "admin"));
        Category category = categoryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                        .eq(Category::getName, "家常菜"));
        StatsOverviewVO before = statsService.overview();

        SysFile image = insertImageFile(admin, "stats-dish.png");
        Dish dish = new Dish();
        dish.setName("统计菜品");
        dish.setImageFileId(image.getId());
        dish.setCategoryId(category.getId());
        dish.setStatus("normal");
        dish.setIsListed(true);
        dishMapper.insert(dish);

        LocalDate dataDate = LocalDate.of(2026, 7, 1);
        insertOrderWithItem(admin, dish, dataDate, "completed", "统计旧名", 2);
        insertOrderWithItem(admin, dish, dataDate, "preparing", "统计新名", 3);
        insertOrderWithItem(admin, dish, dataDate, "cancelled", "取消订单菜名", 10);

        List<TopDishVO> topDishes = statsService.topDishes(dataDate, dataDate, 10);
        assertThat(topDishes).containsExactly(new TopDishVO(dish.getId(), "统计新名", 5));
        List<DailySummaryVO> daily = statsService.dailySummary(dataDate.minusDays(1), dataDate.plusDays(1));
        assertThat(daily).containsExactly(
                new DailySummaryVO(dataDate.minusDays(1), 0, 0),
                new DailySummaryVO(dataDate, 2, 5),
                new DailySummaryVO(dataDate.plusDays(1), 0, 0));

        insertOrderWithItem(admin, dish, LocalDate.now(), "pending", "统计菜品", 1);
        insertOrderWithItem(admin, dish, LocalDate.now(), "cancelled", "统计菜品", 20);
        StatsOverviewVO after = statsService.overview();
        assertThat(after.todayOrders()).isEqualTo(before.todayOrders() + 1);
        assertThat(after.pendingOrders()).isEqualTo(before.pendingOrders() + 1);
        assertThat(after.weekOrders()).isEqualTo(before.weekOrders() + 1);
        assertThat(after.totalDishes()).isEqualTo(before.totalDishes() + 1);
    }

    @Test
    @Transactional
    void exposesOneConsistentApiContractForLoginPagingFieldsAndErrors() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/admin-login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Abc@1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.user").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String adminToken = objectMapper.readTree(loginResponse).path("data").path("token").asText();

        String categoryResponse = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"契约审计分类\",\"sortOrder\":99}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long categoryId = objectMapper.readTree(categoryResponse).path("data").path("id").asLong();
        var insertedAudit = jdbcTemplate.queryForMap("""
                SELECT create_by, update_by FROM dish_category WHERE id = ?
                """, categoryId);
        assertThat(insertedAudit.get("create_by")).isEqualTo("admin");
        assertThat(insertedAudit.get("update_by")).isEqualTo("admin");

        jdbcTemplate.update("""
                UPDATE dish_category
                SET update_time = TIMESTAMP '2000-01-01 00:00:00', update_by = 'stale'
                WHERE id = ?
                """, categoryId);
        mockMvc.perform(put("/api/v1/admin/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"契约审计分类\",\"sortOrder\":98,\"status\":\"active\"}"))
                .andExpect(status().isOk());
        var updatedAudit = jdbcTemplate.queryForMap("""
                SELECT update_time, update_by FROM dish_category WHERE id = ?
                """, categoryId);
        assertThat(updatedAudit.get("update_by")).isEqualTo("admin");
        assertThat(updatedAudit.get("update_time").toString()).doesNotStartWith("2000-01-01");

        mockMvc.perform(get("/api/v1/admin/dishes")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.current").doesNotExist())
                .andExpect(jsonPath("$.data.size").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sortOrder").isNumber())
                .andExpect(jsonPath("$.data[0].deleted").doesNotExist())
                .andExpect(jsonPath("$.data[0].createTime").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/notifications/subscription-config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.templateId").doesNotExist());
        mockMvc.perform(get("/api/v1/user/info")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("pageNum", "0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/api/v1/admin/notifications/{id}/read", 999999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    private SysFile insertImageFile(User owner, String name) {
        SysFile file = new SysFile();
        file.setOriginalName(name);
        file.setFileKey("test/" + name);
        file.setBucket("kitchen");
        file.setSizeBytes(128L);
        file.setMimeType("image/png");
        file.setUploadBy(owner.getId());
        sysFileMapper.insert(file);
        return file;
    }

    private Order insertOrderWithItem(
            User user,
            Dish dish,
            LocalDate mealDate,
            String status,
            String snapshotName,
            int quantity
    ) {
        Order order = new Order();
        order.setUserId(user.getId());
        order.setMealType("dinner");
        order.setMealDate(mealDate);
        order.setStatus(status);
        order.setTasteTags(List.of());
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setDishId(dish.getId());
        item.setDishName(snapshotName);
        item.setQuantity(quantity);
        item.setIsExtra(false);
        orderItemMapper.insert(item);
        return order;
    }

    private SysFile uploadImage(String token, String name) throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "files", name, "image/png", pngBytes());
        String response = mockMvc.perform(multipart("/api/common/file/upload")
                        .file(image)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long fileId = objectMapper.readTree(response).path("data").get(0).path("fileId").asLong();
        return sysFileMapper.selectById(fileId);
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private String adminLogin(String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/admin-login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"%s"}
                                """.formatted(password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("token").asText();
    }
}
