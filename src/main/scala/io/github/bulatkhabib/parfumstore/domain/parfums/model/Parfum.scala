package io.github.bulatkhabib.parfumstore.domain.parfums.model

case class Parfum(
                   name: String,
                   category: String,
                   description: String,
                   price: Int,
                   status: ParfumStatus = ParfumStatus.Available,
                   id: Option[Long] = None
                 ) {
  def asParfum[A]: Parfum = Parfum(
    name,
    category,
    description,
    price,
    status = ParfumStatus.Available
  )
}
