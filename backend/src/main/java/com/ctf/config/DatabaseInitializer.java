package com.ctf.config;

import com.ctf.entity.Category;
import com.ctf.entity.Question;
import com.ctf.entity.User;
import com.ctf.entity.ContestConfig;
import com.ctf.mapper.CategoryMapper;
import com.ctf.mapper.QuestionMapper;
import com.ctf.mapper.UserMapper;
import com.ctf.mapper.ContestConfigMapper;
import com.ctf.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final QuestionMapper questionMapper;
    private final ContestConfigMapper contestConfigMapper;

    @Override
    public void run(String... args) {
        log.info("开始初始化数据库...");
        
        try {
            initializeUsers();
            initializeCategories();
            initializeQuestions();
            initializeContestConfig();
            initializeScoringConfig();
            log.info("数据库初始化完成！");
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage(), e);
        }
    }

    private void initializeUsers() {
        // 检查是否已有管理员账号
        User existingAdmin = userMapper.selectByStudentId("root");
        if (existingAdmin != null) {
            log.info("管理员账号已存在，跳过用户初始化");
            return;
        }

        log.info("创建管理员账号...");
        // 创建管理员账号
        User admin = new User();
        admin.setStudentId("root");
        admin.setUsername("root");
        admin.setPasswordHash(PasswordUtil.encodePassword("admin123"));
        admin.setFullName("系统管理员");
        admin.setRole("admin");
        admin.setIsActive(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(admin);
        log.info("管理员账号创建成功: root / admin123");

        // 创建测试学生账号
        User student = new User();
        student.setStudentId("2024001");
        student.setUsername("student1");
        student.setPasswordHash(PasswordUtil.encodePassword("123456"));
        student.setFullName("测试学生");
        student.setRole("user");
        student.setIsActive(true);
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(student);
        log.info("测试学生账号创建成功: 2024001 / 123456");
    }

    private void initializeCategories() {
        // 检查是否已有分类
        if (!categoryMapper.selectAll().isEmpty()) {
            log.info("题目分类已存在，跳过分类初始化");
            return;
        }

        log.info("创建题目分类...");
        String[][] categories = {
            {"Crypto", "密码学相关题目", "1"},
            {"Misc", "杂项信息相关题目", "2"},
            {"Reverse", "逆向工程相关题目", "3"},
            {"Web", "网络安全相关题目", "4"},
            {"Pwn", "二进制漏洞相关题目", "5"}
        };

        for (String[] cat : categories) {
            Category category = new Category();
            category.setName(cat[0]);
            category.setDescription(cat[1]);
            category.setOrderNum(Integer.parseInt(cat[2]));
            category.setIsActive(true);
            category.setCreatedAt(LocalDateTime.now());
            category.setUpdatedAt(LocalDateTime.now());
            categoryMapper.insert(category);
        }
        log.info("创建了 {} 个题目分类", categories.length);
    }

    private void initializeQuestions() {
        // 检查是否已有题目
        if (!questionMapper.selectAll().isEmpty()) {
            log.info("题目已存在，跳过题目初始化");
            return;
        }

        log.info("创建示例题目...");
        Object[][] questions = {
            // categoryId, title, description, flag, difficulty, orderNum
            {1, "凯撒密码", "破解凯撒密码: KHOOR ZRUOG", "HELLO WORLD", "easy", 1},
            {1, "简单密码", "破解简单替换密码", "CTF{CRYPTO}", "medium", 2},
            
            {2, "隐写术", "找出隐藏在图片中的信息", "FLAG{STEGANOGRAPHY}", "hard", 1},
            {2, "元数据", "从文件元数据中提取信息", "secret123", "medium", 2},
            
            {3, "逆向初级", "分析程序找出Flag", "flag{reverse_me}", "medium", 1},
            {3, "逆向进阶", "分析复杂程序结构", "ADVANCED_FLAG", "hard", 2},
            
            {4, "SQL注入", "利用SQL注入获取数据", "1' OR '1'='1", "medium", 1},
            {4, "XSS漏洞", "演示XSS漏洞利用", "<script>alert(1)</script>", "hard", 2},
            
            {5, "Buffer Overflow", "溢出缓冲区获取权限", "buffer_overflow_flag", "hard", 1},
            {5, "栈溢出", "栈溢出利用实践", "pwn_me", "hard", 2}
        };

        for (Object[] q : questions) {
            Question question = new Question();
            question.setCategoryId((Integer) q[0]);
            question.setTitle((String) q[1]);
            question.setDescription((String) q[2]);
            question.setFlag((String) q[3]);
            question.setPoints(defaultPointsByDifficulty((String) q[4]));
            question.setDifficulty((String) q[4]);
            question.setOrderNum((Integer) q[5]);
            question.setIsActive(true);
            question.setCreatedAt(LocalDateTime.now());
            question.setUpdatedAt(LocalDateTime.now());
            questionMapper.insert(question);
        }
        log.info("创建了 {} 道示例题目", questions.length);
    }

    private int defaultPointsByDifficulty(String difficulty) {
        if ("easy".equals(difficulty)) {
            return 100;
        }
        if ("hard".equals(difficulty)) {
            return 300;
        }
        return 200;
    }
    
    private void initializeContestConfig() {
        // 检查是否已有配置
        if (contestConfigMapper.selectByKey("contest.startTime") != null) {
            log.info("比赛配置已存在，跳过配置初始化");
            return;
        }

        log.info("初始化比赛配置...");
        // 默认时间：明天的15:20-17:00
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        String readyTime = tomorrow.withHour(15).withMinute(20).withSecond(0).format(formatter);
        String startTime = tomorrow.withHour(15).withMinute(30).withSecond(0).format(formatter);
        String endTime = tomorrow.withHour(16).withMinute(30).withSecond(0).format(formatter);
        String resultsTime = tomorrow.withHour(17).withMinute(0).withSecond(0).format(formatter);
        
        upsertConfig("contest.readyTime", readyTime);
        upsertConfig("contest.startTime", startTime);
        upsertConfig("contest.endTime", endTime);
        upsertConfig("contest.resultsTime", resultsTime);
        
        log.info("比赛配置初始化完成，默认开始时间: {}", startTime);
    }

    private void initializeScoringConfig() {
        insertIfAbsent("scoring.min_points", "1");
        insertIfAbsent("scoring.decay_step", "0");
        insertIfAbsent("scoring.first_blood_bonus", "0");
        insertIfAbsent("scoring.freeze_on_end", "true");
        insertIfAbsent("scoring.overview_timezone", "Asia/Shanghai");
        log.info("动态计分配置初始化完成");
    }

    private void insertIfAbsent(String key, String value) {
        if (contestConfigMapper.selectByKey(key) == null) {
            upsertConfig(key, value);
        }
    }
    
    private void upsertConfig(String key, String value) {
        ContestConfig config = ContestConfig.builder()
                .configKey(key)
                .configValue(value)
                .build();
        contestConfigMapper.upsert(config);
    }
}
