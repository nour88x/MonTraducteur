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

# 3. DEBUG ET COPIE (La partie corrigée)
# On affiche la liste des fichiers pour être sûr (apparaîtra dans les logs de build)
RUN ls -l target/

# On copie le fichier EXACT (basé sur ton pom.xml)
# Si cette ligne échoue, c'est que le fichier n'a pas été créé
RUN cp target/traducteur-0.0.1-SNAPSHOT.jar app.jar

# 4. Lancement
CMD ["java", "-Dserver.port=8081", "-Xmx256m", "-jar", "app.jar"]