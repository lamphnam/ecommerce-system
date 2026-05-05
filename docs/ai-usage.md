# AI Usage Disclosure

This document explains how AI assistance was used during this project. The project was designed, implemented, tested, and documented under my direction. AI was used as a supporting tool for development productivity, debugging, documentation, and review. It was not used as a replacement for understanding the system design or for making the final technical decisions.

## Tools used

I used Claude Code during the development process. It helped with repository inspection, implementation suggestions, code edits, local command execution, test-output review, and documentation drafting.

All AI-generated changes were reviewed before being kept in the project.

## How AI was used

### 1. Architecture planning

AI was used to help organize and refine the RabbitMQ-based microservices architecture. This included discussing:

- why RabbitMQ is suitable for this order-processing workflow
- how to separate responsibilities between API Gateway, Order Service, Inventory Service, Payment Service, Notification Service, and Analytics Service
- how asynchronous messaging reduces coupling compared with synchronous REST calls
- how to model the order flow using events such as `order.created`, `inventory.reserved`, `payment.requested`, `payment.succeeded`, `payment.failed`, and `notification.requested`
- how to compare RabbitMQ with Apache Kafka and Apache ActiveMQ in the context of this assignment

The final technology choice, system direction, and architectural trade-offs were decided by me. AI was mainly used to challenge, organize, and improve the explanation.

### 2. Source-code implementation support

AI assistance was used while implementing and reviewing parts of the Spring Boot codebase, including:

- RabbitMQ exchange, queue, routing-key, and DLQ configuration
- asynchronous order processing through domain events
- synchronous order baseline endpoint for comparison experiments
- payment and inventory synchronous endpoints used by the baseline flow
- JWT handling in the API Gateway and downstream `X-User-*` header propagation
- CORS configuration in the API Gateway
- OpenAPI/Swagger configuration through the gateway
- idempotency for order creation and event consumption
- error handling and service-layer behavior

AI helped generate and modify some code, but the implementation was checked against the actual running services. I reviewed the changes, ran the application locally, fixed issues when they appeared, and validated the behavior with API calls and k6 scripts.

### 3. Debugging and runtime fixes

AI was used to help investigate issues found during local execution and performance testing. Examples include:

- mismatches between k6 scripts and the actual API request/response contracts
- login token extraction from the gateway authentication response
- order and inventory request payload issues
- protected API calls in the DLQ experiment script
- service startup and health-check problems
- product ID mismatches in the k6 scripts
- separating real script errors from expected threshold failures during load tests

The fixes were not accepted blindly. I checked them against source code, service responses, logs, and generated test results before keeping them.

### 4. Experiment and performance-test preparation

AI was used to help create and refine k6 experiment scripts under `experiments/`, including:

- normal-load async vs sync comparison
- spike testing
- slow-provider behavior
- provider-failure behavior
- idempotency behavior
- DLQ behavior
- recovery and queue-depth observation

AI also helped organize the k6 outputs into a written experiment report. The raw outputs are stored under `results/`, and the interpreted report is available in `docs/experiment-report.md`.

Some experiment results were intentionally marked as partial or projected when the required failure precondition was not fully applied during the run. I kept this distinction to avoid overstating what the experiment actually proved.

### 5. Documentation support

AI was used to help draft and polish project documentation, including:

- `README.md`
- `docs/architecture.md`
- `docs/database.md`
- `docs/message-flow.md`
- `docs/experiment-report.md`
- `docs/why-rabbitmq.md`

The documentation was based on the actual project structure, source code, Docker configuration, RabbitMQ setup, and k6 result files. AI helped with wording, structure, tables, and Mermaid diagrams, while I checked the content against the implemented system.

### 6. Review and quality checking

Before submission, AI was also used as a technical reviewer. The review focused on:

- whether the project satisfies the assignment requirements
- whether RabbitMQ is properly justified against Kafka and ActiveMQ
- whether the architecture covers retry, DLQ, idempotency, service decoupling, and failure handling
- whether the experiment evidence is clear and not overstated
- whether the documentation honestly explains current limitations

This review helped identify areas to improve, such as Kubernetes deployment documentation, monitoring discussion, experiment limitations, and the transactional outbox pattern as a production hardening step.

## Human contribution

I was responsible for:

- choosing the RabbitMQ-based direction for the project
- designing the main service flow and event-driven architecture
- implementing and integrating the Spring Boot microservices
- running the local stack and debugging runtime issues
- executing the k6 experiments
- validating API behavior against the running services
- reviewing and deciding which AI suggestions to keep
- writing and finalizing the submission materials
- ensuring the final documentation matches what was actually implemented and measured

## Limitations of AI assistance

AI suggestions were treated as drafts or recommendations, not as final answers. They required verification because AI can produce incomplete, generic, or inaccurate suggestions.

During this project, AI output was checked against:

- Java source code
- Spring Boot runtime behavior
- Docker, RabbitMQ, and PostgreSQL setup
- API responses
- k6 execution results
- project documentation

Where measured evidence was incomplete, the documentation separates real measured results from expected or projected behavior.

## Summary

AI was used as a development assistant for coding, debugging, testing, documentation, and review. The project remains a human-directed submission. The system design, implementation validation, experiment execution, and final responsibility for correctness belong to me.