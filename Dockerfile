FROM eclipse-temurin:17-jdk-focal

# 1. Installation des dépendances (Tesseract)
RUN apt-get update && \
    apt-get install -y \
    maven \
    tesseract-ocr \
    tesseract-ocr-fra \
    tesseract-ocr-ara \
    tesseract-ocr-spa \
    tesseract-ocr-tur \
    tesseract-ocr-eng && \
    apt-get clean

# 2. Configuration dossier Tesseract
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/tessdata

# 3. Création d'un utilisateur spécial (Obligatoire pour Hugging Face)
RUN useradd -m -u 1000 user
WORKDIR /app

# 4. Copie des fichiers
COPY --chown=user . .

# 5. Construction du projet
# On force le nom "app" pour ne pas avoir d'erreur "jarfile not found"
RUN mvn clean package -DskipTests -DfinalName=app

# 6. On passe sur l'utilisateur sécurisé
USER user

# 7. Lancement sur le port 7860 (OBLIGATOIRE CHEZ HUGGING FACE)
CMD ["java", "-Dserver.port=7860", "-Xmx1024m", "-jar", "target/app.jar"]