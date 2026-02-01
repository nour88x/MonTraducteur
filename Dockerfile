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

WORKDIR /app
COPY . .

# 2. Construction du projet avec NOM FORCÉ
# L'option -DfinalName=app oblige Maven à créer le fichier "target/app.jar"
RUN mvn clean package -DskipTests -DfinalName=app

# 3. Vérification (pour les logs, au cas où)
RUN ls -l target/

# 4. Lancement
# On pointe directement sur le fichier créé par Maven
CMD ["java", "-Dserver.port=8081", "-Xmx256m", "-jar", "target/app.jar"]