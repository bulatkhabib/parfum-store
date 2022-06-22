package io.github.bulatkhabib.parfumstore.domain.carts.model

case class Cart(
                 userId: Long,
                 itemId: Long,
                 id: Option[Long] = None
               ) {
  def asCart[A](userId: Long, cartId: Long): Cart = Cart(
    userId,
    itemId
  )
}
