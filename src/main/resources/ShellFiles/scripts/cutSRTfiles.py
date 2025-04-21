import pysrt

def is_end_of_sentence(text):
    return text.strip()[-1:] in '.!?'

def split_subtitles(subs, min_len=4, max_len=6):
    chunks = []
    chunk = []

    i = 0
    while i < len(subs):
        chunk_start = i
        chunk = []
        while i < len(subs) and len(chunk) < max_len:
            chunk.append(subs[i])
            i += 1
            if len(chunk) >= min_len and is_end_of_sentence(chunk[-1].text):
                break

        if i == len(subs) and len(chunk) < min_len and chunks:
            chunks[-1].extend(chunk)
        else:
            chunks.append(chunk)

    return [pysrt.SubRipFile(items=chunk) for chunk in chunks]

def print_chunk_ranges(chunks):
    for idx, chunk in enumerate(chunks, 1):
        start_id = chunk[0].index
        end_id = chunk[-1].index
        print(f"Фрагмент {idx}: субтитри {start_id}–{end_id}")

# Завантаження субтитрів
subs = pysrt.open('english.srt', encoding='utf-8')

# Отримання фрагментів у пам'яті
chunks_in_memory = split_subtitles(subs)

# Вивід діапазонів у консоль
print_chunk_ranges(chunks_in_memory)

# Тепер chunks_in_memory — список об'єктів SubRipFile
