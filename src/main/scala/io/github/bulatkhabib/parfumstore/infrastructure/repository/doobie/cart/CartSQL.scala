package io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.cart

import doobie._
import doobie.implicits._
import io.github.bulatkhabib.parfumstore.domain.carts.model.Cart
import io.github.bulatkhabib.parfumstore.domain.carts.service.CartRepositoryAlgebra

private object CartSQL {

  def insert(cart: Cart): Update0 = sql"""
    INSERT INTO CARTS (USER_ID, ITEM_ID)
    VALUES (${cart.userId}, ${cart.itemId})
  """.update

  def delete(itemId: Long, userId: Long): Update0 = sql"""
    DELETE FROM CARTS WHERE ITEM_ID = $itemId AND USER_ID = $userId
  """.update

  def selectAll(userId: Long): Query0[Cart] = sql"""
    SELECT USER_ID, ITEM_ID, ID
    FROM CARTS
    WHERE USER_ID = $userId
  """.query

  def select(itemId: Long): Query0[Cart] = sql"""
    SELECT USER_ID, ITEM_ID, ID
    FROM CARTS
    WHERE ITEM_ID = $itemId
  """.query
}