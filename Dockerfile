# Utilise une image de base Java 17
FROM eclipse-temurin:17-jdk-focal

# 1. Installation de Tesseract, des langues ET de MAVEN (La correction est ici)
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

# 2. Variable Tesseract
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/tessdata

# Préparation du dossier
WORKDIR /app
COPY . .

# 3. Construction avec le Maven installé dans Docker (et plus via mvnw)
# On utilise "mvn" directement au lieu de "./mvnw"
RUN mvn clean package -DskipTests

# 4. Lancement
CMD ["java", "-Dserver.port=8081", "-Xmx350m", "-jar", "target/traducteur-0.0.1-SNAPSHOT.jar"]