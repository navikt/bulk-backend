FROM navikt/java:17

ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:MaxPermSize=256m"

COPY build/libs/*.jar ./
