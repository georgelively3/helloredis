h2. Redis Deployment Modes

This reference application supports two Redis deployment models:

|| Mode || Use Case || Authentication ||
| Local Redis | Developer workstations, Testcontainers integration tests | None or static password |
| AWS ElastiCache | Cloud deployments | IAM authentication using SigV4-generated tokens |

By default, the application uses Spring Boot's standard Redis auto-configuration.

To enable AWS ElastiCache IAM authentication:

{code:bash}
AWS_ELASTICACHE_ENABLED=true
{code}

When enabled, a custom *LettuceConnectionFactory* is created that:

* Connects to a Redis cluster endpoint
* Uses TLS/SSL
* Generates an AWS SigV4 authentication token
* Authenticates as an ElastiCache IAM user

---

h2. AWS ElastiCache Configuration

When running against AWS ElastiCache with IAM authentication, the following configuration properties are required.

{code:yaml}
aws:
elasticache:
enabled: true
cluster-endpoint: your-cluster-endpoint.cache.amazonaws.com
cluster-name: your-cluster-name
region: us-east-1
port: 6379
iam-username: appuser
{code}

|| Property || Description ||
| aws.elasticache.enabled | Enables ElastiCache IAM authentication mode |
| aws.elasticache.cluster-endpoint | Redis cluster endpoint |
| aws.elasticache.cluster-name | ElastiCache cluster name |
| aws.elasticache.region | AWS region |
| aws.elasticache.port | Redis TLS port |
| aws.elasticache.iam-username | IAM-enabled Redis username |

These values should typically be injected through Helm values, Kubernetes environment variables, or platform configuration.

---

h2. RedisConfig

The application contains a custom *RedisConfig* class that conditionally replaces Spring Boot's default Redis configuration when ElastiCache mode is enabled.

Key responsibilities:

* Enables Spring Data Redis repositories
* Generates an IAM authentication token
* Configures Redis Cluster connectivity
* Enables TLS/SSL
* Creates a LettuceConnectionFactory

The bean is activated only when:

{code}
aws.elasticache.enabled=true
{code}

Otherwise, Spring Boot falls back to the standard Redis configuration defined in *application.yml*.

This allows the same application binary to run:

* Locally against Redis
* In Testcontainers integration tests
* Against AWS ElastiCache

without code changes.

---

h2. ElastiCache IAM Authentication

AWS ElastiCache IAM authentication does not use a static password.

Instead, the application generates a temporary authentication token using AWS Signature Version 4 (SigV4).

The *ElastiCacheIAMTokenProvider* performs the following steps:

# Retrieves AWS credentials using the AWS SDK DefaultCredentialsProvider

# Creates a SigV4-signed request

# Generates a pre-signed authentication token

# Uses the token as the Redis password

This behavior is equivalent to Python's:

{code:python}
ElastiCacheIAMProvider(...)
{code}

The generated token is supplied to Redis as the AUTH credential.

---

h2. AWS Credential Sources

The token provider uses:

{code:java}
DefaultCredentialsProvider
{code}

This means credentials may come from:

* EKS IAM Roles for Service Accounts (IRSA)
* EC2 instance profiles
* ECS task roles
* Environment variables
* Local AWS profiles

No AWS access keys should be hardcoded in the application.

---

h2. Token Expiration Considerations

AWS ElastiCache IAM tokens expire after 15 minutes.

The reference implementation generates a token during startup and uses it to establish the Redis connection.

This approach is sufficient for:

* Reference implementations
* Demonstrations
* Short-lived workloads
* Proof-of-concept applications

Production systems should implement token rotation using one of the following approaches:

* Scheduled recreation of the LettuceConnectionFactory
* Lettuce RedisCredentialsProvider support
* Custom credential refresh logic

Without rotation, long-running applications may eventually lose connectivity when the token expires.

---

h2. TLS Configuration

The ElastiCache connection factory enables TLS by default.

{code:java}
.useSsl()
{code}

The reference application also contains:

{code:java}
.disablePeerVerification()
{code}

This mirrors the Python sample:

{code:python}
ssl_cert_reqs=None
{code}

For production environments, certificate validation should normally remain enabled and trusted CA certificates should be configured appropriately.

---

h2. Platform Integration

For platform deployments, the following values are typically provided through BOM and Helm configuration:

* Redis cluster endpoint
* Cluster name
* AWS region
* IAM username
* TLS port

The application code remains unchanged across environments.

The platform is responsible for:

* Injecting configuration values
* Providing AWS credentials through IRSA or equivalent mechanisms
* Managing Redis infrastructure

The application is responsible only for:

* Generating IAM authentication tokens
* Establishing Redis connections
* Executing Redis operations through Spring Data Redis
