Please keep in mind, that what you see in this project is not what you should be doing _exactly_.
It's more of a guideline, to grasp some ideas and them apply them in your real projects.

Why don't you start with 

    ./mvnw test

Perhaps it's wiser to run slow tests only after fast tests pass?

    ./mvnw clean test '-Dtest=!TestInt*' && ./mvnw clean test '-Dtest=TestInt*'

Perhaps the containers should start only once?\
You can do that by making all `@Container` fields `static`.

Next, do we really need to start containers one by one?\
Nope, we can make Testcontainers parallel: `@Testcontainers(parallel = true)`.

Wait, aren't we still starting containers too often?\
Maybe a `static` block is even better than `static` fields.

Do we really need to run DB migrations before every test?\
Nope, we can have a snapshot of the initial state in the container and simply restore it. Maybe we can even cache it somehow?
