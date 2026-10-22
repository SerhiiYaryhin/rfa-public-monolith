#!/bin/bash

OUTPUT="audit.txt"
> "$OUTPUT" # Очищаємо файл

echo "🔍 Починаю аудит..."

# 1. Повна структура каталогів і файлів
echo "=== 1. СТРУКТУРА КАТАЛОГІВ І ФАЙЛІВ (ls -laR) ===" >> "$OUTPUT"
# Виводимо все, крім стандартного сміття від IDE та білду
ls -laR | grep -v -E '\.git|node_modules|\.idea|\.vscode|target|build|bin|out' >> "$OUTPUT"

# 2. Пошук ключових Java-анотацій
echo -e "\n\n=== 2. КЛЮЧОВІ JAVA АНОТАЦІЇ ===" >> "$OUTPUT"
grep -rn -E '@RestController|@Controller|@Service|@Component|@RabbitListener|@MessageMapping|@Configuration|@Entity|@Bean|@EventListener' --include="*.java" . | grep -v -E '\.git|node_modules|target|build' >> "$OUTPUT"

# 3. Початок знайдених файлів (перші 40 рядків, щоб побачити package, imports та клас)
echo -e "\n\n=== 3. ПОЧАТОК ФАЙЛІВ З АНОТАЦІЯМИ ===" >> "$OUTPUT"
grep -rn -E '@RestController|@Controller|@Service|@Component|@RabbitListener|@MessageMapping|@Configuration|@Entity|@Bean|@EventListener' --include="*.java" . | grep -v -E '\.git|node_modules|target|build' | cut -d: -f1 | sort | uniq | while read -r file; do
    echo -e "\n--- ФАЙЛ: $file ---" >> "$OUTPUT"
    head -n 40 "$file" >> "$OUTPUT"
done

echo "✅ Готово! Результати збережено в $OUTPUT"
