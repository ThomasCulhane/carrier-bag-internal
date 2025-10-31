set -euo pipefail


mkdir -p lib
cd lib

echo "Library Download Directory: $(pwd)"

curl -L \
    -O https://repo1.maven.org/maven2/org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar \
    -O https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar \
    -O https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar \
    -O https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar \
    -O https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-api/0.13.0/jjwt-api-0.13.0.jar \
    -O https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-impl/0.13.0/jjwt-impl-0.13.0.jar \
    -O https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-jackson/0.13.0/jjwt-jackson-0.13.0.jar \
    -O https://repo1.maven.org/maven2/org/springframework/security/spring-security-crypto/7.0.0-RC1/spring-security-crypto-7.0.0-RC1.jar

echo "Downloaded libraries:"
ls -lh