package io.github.bulatkhabib.parfumstore.config

final case class ServerConfig(host: String, port: Int)
final case class ParfumStoreConfig(db: DatabaseConfig, server: ServerConfig)
