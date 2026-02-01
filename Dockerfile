FROM eclipse-temurin:17-jdk-focal

# 1. Installation des dépendances
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

ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/tessdata

# 2. Création utilisateur (OBLIGATOIRE POUR HUGGING FACE)
RUN useradd -m -u 1000 user
WORKDIR /app

# 3. Copie des fichiers
COPY --chown=user . .

# 4. Construction (Force le nom 'app.jar')
RUN mvn clean package -DskipTests -DfinalName=app

# 5. Permission et Port 7860 (TRES IMPORTANT)
USER user
ENV PORT=7860
EXPOSE 7860

# 6. Lancement sur le port 7860
CMD ["java", "-Dserver.port=7860", "-Xmx1024m", "-jar", "target/app.jar"]