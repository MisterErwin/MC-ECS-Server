A simple mc server using events and an Entity Component System (ECS)

I should write a bit more... I guess

This project uses mainly:
 * [Artemis-odb](https://github.com/junkdog/artemis-odb) as an Entity Component System framework
 * [MCProtocolLib](https://github.com/Steveice10/MCProtocolLib) for protocol related things
 * [MBassador](https://github.com/bennidi/mbassador) as an event bus

Start it yourself:
 * Download the minecraft server jar of whatever MCProtocolLib version is used
 * Run the [minecraft data generators](https://wiki.vg/Data_Generators) with the `--server --reports` parameters and copy its reports so that the file `resources/reports/blocks.json` exists
 * Build using maven
   * The [MCECSProtocolDataGenerator](https://github.com/MisterErwin/MCECSProtocolDataGenerator) tool then generates its classes from the reports
 * Finally, run the ServerMain 
