# A Presença (Fabric 1.21.1)

## Compilar
1. Instale o JDK 21.
2. Copie a pasta `gradle/` + `gradlew` + `gradlew.bat` do mod de exemplo oficial do Fabric (fabricmc.net/develop/template) para esta pasta
   (ou rode `gradle wrapper --gradle-version 8.10` se tiver Gradle instalado).
3. `./gradlew build` -> o .jar fica em `build/libs/presenca-1.0.0.jar`.
4. Coloque o .jar na pasta `mods` do servidor E de cada jogador, junto com o Fabric API.

## Comandos (OP)
/presenca spawn [jogador]
/presenca limpar
/presenca toggle
/presenca sanidade get|set <jogador> [0-100]
/presenca evento apagao|passos|batimentos|sussurro|susto <jogador>

Config: config/presenca.json

## Sem instalar nada (GitHub)
1. Crie um repositório no github.com e suba TODOS os arquivos desta pasta (incluindo .github).
2. Aba "Actions" -> "Compilar mod" -> espere ficar verde.
3. Clique na execução -> "Artifacts" -> baixe "presenca-jar" -> o .jar (sem "-sources") é o mod.
