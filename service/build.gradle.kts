import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.0"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.spring") version "2.1.20"
    kotlin("plugin.jpa") version "2.1.20"
}

group = "com.example" // 그룹 ID
version = "0.0.1-SNAPSHOT" // 버전
java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral() // Maven Central Repository: 의존성을 다운로드할 저장소
    // 만약 다른 저장소를 사용하고 싶다면 다음과 같이 추가 가능
    // maven { url "https://repo.spring.io/milestone" } // Spring Milestone Repository (예시)
}

dependencies {
    // 웹 관련 의존성 (REST API 및 파일 업로드 지원)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // JPA / H2
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")

    // Reactive support (StreamingController에서 RandomAccessFile 처리 없이 webflux 사용 가능)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // 테스트 관련 의존성
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// Kotlin 컴파일러 옵션 설정
tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21" // JVM 타겟 버전 설정
        freeCompilerArgs = listOf("-Xjsr305=strict") // JSR-305 annotataion을 엄격하게 처리
    }
}

// 테스트 태스크 설정
tasks.withType<Test> {
    useJUnitPlatform()
}
