FROM eclipse-temurin:17-jdk-focal

# 1. Installation des dépendances (Maven + Tesseract)
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

WORKDIR /app
COPY . .

# 2. Construction du projet
RUN mvn clean package -DskipTests

# 3. ASTUCE : On renomme le fichier généré en "app.jar" pour être sûr du nom
RUN mv target/*.jar app.jar

# 4. Lancement
# On force le port 8081 ici pour être sûr que ça matche avec Koyeb
CMD ["java", "-Dserver.port=8081", "-Xmx350m", "-jar", "app.jar"]