FROM eclipse-temurin:17-jdk-focal

# 1. Installation dépendances
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

# 2. Création utilisateur
RUN useradd -m -u 1000 user
WORKDIR /app

# 3. Copie des fichiers
COPY --chown=user . .

# 4. Construction
# On enlève -DfinalName pour laisser Maven faire ce qu'il veut par défaut
RUN mvn clean package -DskipTests

# 5. === LA CORRECTION ===
# On affiche ce qu'il y a dans 'target' pour comprendre (regarde les logs si ça plante !)
RUN ls -lR target/

# Au lieu de chercher un nom précis, on cherche TOUT fichier finissant par .jar
# et on le renomme en /app/app.jar
RUN find target -name "*.jar" -exec cp {} /app/app.jar \;

# On donne les droits
RUN chown user:user /app/app.jar

# 6. Config finale
USER user
ENV PORT=7860
EXPOSE 7860

CMD ["java", "-Dserver.port=7860", "-Xmx2G", "-jar", "/app/app.jar"]