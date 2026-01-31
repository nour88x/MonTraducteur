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

# 2. Construction du projet
RUN mvn clean package -DskipTests

# 3. Renommage sécurisé (prend le plus gros fichier jar trouvé)
# Cela évite les erreurs si le nom change
RUN find target -name "*.jar" -type f -size +1M -exec cp {} app.jar \;

# 4. Lancement OPTIMISÉ MÉMOIRE
# -Xmx256m : On limite Java à 256Mo pour ne pas faire exploser le conteneur de 512Mo
# -Dserver.port=8081 : On force le port 8081
CMD ["java", "-Dserver.port=8081", "-Xmx256m", "-jar", "app.jar"]