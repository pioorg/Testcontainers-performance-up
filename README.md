## Intro
Please keep in mind, that what you see in this project is not what you should be doing _exactly_. And _always_.
It's more of a guideline, to grasp some ideas and them apply them in your real projects.
This project requires Java™ 21.

Why don't you start with 

    ./mvnw test

### Task 1
Perhaps it's wiser to run slow tests only after fast tests pass?

Tests can be split logically in many ways, one of them is running them separately, e.g.

    ./mvnw test '-Dtest=!TestInt*' && ./mvnw test '-Dtest=TestInt*'

### Task 2
Perhaps the containers should start only once?