package org.thp.thehive.connector.misp

import org.thp.scalligraph.models.Database
import org.thp.scalligraph.{ScalligraphApplication, ScalligraphApplicationImpl}
import org.thp.thehive.connector.misp.services.{TestMispClientProvider, TheHiveMispClient}
import org.thp.thehive.services.{TheHiveTestModule, WithTheHiveModule}
import org.thp.thehive._
import play.api.Mode

import java.io.File
import scala.util.Try

trait TestAppBuilder extends LogFileConfig {
  val databaseName: String      = "default"
  val testApplicationNoDatabase = new ScalligraphApplicationImpl(new File("."), getClass.getClassLoader, Mode.Test)

  def buildTheHiveModule(app: ScalligraphApplication): TheHiveModule = new TheHiveTestModule(app)

  def buildMispConnector(app: ScalligraphApplication): MispTestModule = new MispTestModule(app)

  def buildDatabase(db: Database): Try[Unit] = new DatabaseBuilderModule(buildApp(db)).databaseBuilder.build(db)

  def buildApp(db: Database): TestApplication with WithTheHiveModule with WithMispConnector =
    new TestApplication(db, testApplicationNoDatabase) with WithTheHiveModule with WithMispConnector {
      override val thehiveModule: TheHiveModule = buildTheHiveModule(this)
      injectModule(thehiveModule)
      override val mispConnector: MispTestModule = buildMispConnector(this)
      injectModule(mispConnector)
    }

  def testApp[A](body: TestApplication with WithTheHiveModule with WithMispConnector => A): A =
    JanusDatabaseProvider
      .withDatabase(databaseName, buildDatabase, testApplicationNoDatabase.actorSystem) { db =>
        body(buildApp(db))
      }
      .get
}

trait WithMispConnector {
  val mispConnector: MispTestModule
}

class MispTestModule(app: ScalligraphApplication) extends MispModule(app) {

  import com.softwaremill.macwire._

  lazy val theHiveMispClient: TheHiveMispClient = wire[TestMispClientProvider].get()
}