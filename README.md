A simple mc server using events and an Entity Component System (ECS)

I should write a bit more... I guess

This project uses mainly:
 * [Artemis-odb](https://github.com/junkdog/artemis-odb) as an Entity Component System framework
 * [MCProtocolLib](https://github.com/Steveice10/MCProtocolLib) for protocol related things
 * [MBassador](https://github.com/bennidi/mbassador) as an event bus

Start it yourself:
 * Download the 21w17a minecraft server jar of whatever MCProtocolLib version is used
 * Run the [minecraft data generators](https://wiki.vg/Data_Generators) with the `--server --reports` parameters and copy its reports so that the file `./src/main/resources/reports/blocks.json` exists (relative from the main directory)
    * e.g., download `curl -O https://launcher.mojang.com/v1/objects/ec995f939bb41a785f960985e73821c7044fc32e/server.jar` (21w17a)
    * `java -cp server.jar net.minecraft.data.Main --server --reports`
    * `mv generated/reports/* src/main/resources/reports`
 * Build using maven (`mvn package`)
   * The [MCECSProtocolDataGenerator](https://github.com/MisterErwin/MCECSProtocolDataGenerator) tool then generates its classes from the reports
 * Finally, run the ServerMain: `java -cp target/ecs-server-1.0-SNAPSHOT-jar-with-dependencies.jar es.luepg.ecs.ServerMain` and connect to `localhost:25565`

