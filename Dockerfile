# Utilise une image de base légère mais complète
FROM eclipse-temurin:17-jdk-focal

# 1. Installation de Tesseract et des langues
RUN apt-get update && \
    apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-fra \
    tesseract-ocr-ara \
    tesseract-ocr-spa \
    tesseract-ocr-tur \
    tesseract-ocr-eng && \
    apt-get clean

# 2. Définir la variable d'environnement pour que Java trouve Tesseract
# C'est LA solution au problème de chemin introuvable
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/tessdata

# Préparation du dossier
WORKDIR /app
COPY . .

# 3. Rendre le script Maven exécutable et builder
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# 4. Lancement optimisé pour la RAM (Render gratuit = 512Mo RAM)
# On limite Java à 350Mo pour laisser de la place à Tesseract et au système
CMD ["java", "-Xmx350m", "-jar", "target/traducteur-0.0.1-SNAPSHOT.jar"]