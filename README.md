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

You can do that by making all `@Container` fields `static`. And making sure the state of the containers is cleaned between the tests.

### Task 3
Next, do we really need to start containers one by one?

Nope, we can make Testcontainers start containers in parallel: `@Testcontainers(parallel = true)`.

### Task 4
Wait, aren't we still starting containers too often?

Maybe a `static` block is even better than `static` fields.

### Task 5
Do we really need to run DB migrations before every test?

Nope, we can have a snapshot of the initial state in the container and simply restore it before next test.

### Task 6
What should we actually start with, before we start improving things?

Establishing a baseline. And we can use JFR for that.

### Task 7
Can we do something with the knowledge we have now?

Sure thing! We can squeeze more from our CPU (if we have more CPU)! How about

    ./mvnw test '-Dtest=TestInt*' -DforkCount=2

