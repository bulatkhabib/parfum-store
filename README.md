# parfum-store
 
To start the project you need to write docker-compose up and press play
 
 
- USERS Endpoint

For unauthed users it's 2 endpoints - signup and login
Only admin can use 4 endpoints - update, list, search by email, delete

1) for SIGNUP user 

        POST http://localhost:8080/users/signup
    
        {
            "firstName" : "bulat",
            "lastName" : "bulatik",
            "email" : "bulat@mail.ru",
            "password" : "1234",
            "phone" : "1234657"
        }

    by default the role is will be "Customer", for change it we have to go to DB and change it manually
  
 
2) for LOGIN user
    
        POST http://localhost:8080/users/login

        admin's account
   
        {
            "email" : "1234@mail.ru",
            "password" : "1234"
        }

        --------------------------------
        created user's account

        {
            "email" : "test@mail.ru",
            "password" : "1234"
        }

3) for UPDATE user we have to login by admin

        PUT http://localhost:8080/users/{email}

        {
            "firstName": "test1234",
            "lastName": "test1234",
            "email": "test@mail.ru",
            "hash": "$2a$10$pyQWQmitBEFYCeTSwL4LhOg4/UwkdUWKgNqjXe1fLijNdOkc57TGq",
            "phone": "1234567",
            "id": 2,
            "role": "Customer"
        }

4) for see LIST users we have to login by admin 

        GET http://localhost:8080/users

        we can put pageSize http://localhost:8080/users?pageSize=2

5) for SEARCH user by email we have to login by admin 

        GET http://localhost:8080/users/{email}

6) for DELETE user by email we have to login by admin

        GET http://localhost:8080/users/{email}

---
- PARFUM Endpoint

For unauthed users it's 3 endpoints - getParfum, findParfumByStatus, list
Only admin can use 3 endpoints - create, update, delete

1) for CREATE parfum we have to login by admin

        POST http://localhost:8080/parfums

        {
            "name" : "Lavanda",
            "category" : "Woman",
            "description" : "Amazing",
            "price" : 15000,
            "status" : "Available"
        }

2) for UPDATE parfum we have to login by admin

        PUT http://localhost:8080/parfums

        {
            "name" : "Lavanda",
            "category" : "Woman",
            "description" : "Amazing",
            "price" : 30000,
            "status" : "Available",
            "id" : 5
        }

3) for DELETE parfum we have to login by admin

        DELETE http://localhost:8080/parfums/{id}

4) for GET parfum by id 

        GET http://localhost:8080/parfums/{id}

5) for FIND parfums by status 
    
        GET http://localhost:8080/parfums?status="Available"

6) for GET parfums list 

        GET http://localhost:8080/parfums

--- 

- CARTS Endpoint

All roles can use all endpoints

1) for CREATE cart with parfum 

        POST http://localhost:8080/carts/{id}

2) for GET your cart 

        GET http://localhost:8080/carts

3) for DELETE parfum from cart

        DELETE http://localhost:8080/carts/{id}
