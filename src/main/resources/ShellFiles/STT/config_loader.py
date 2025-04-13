import yaml

def load_config(filename="STTconfig.yaml"):
    """ Завантаження конфігурації з YAML-файлу """
    with open(filename, "r") as file:
        return yaml.safe_load(file)

# Завантаження конфігурації при імпорті
config = load_config()
