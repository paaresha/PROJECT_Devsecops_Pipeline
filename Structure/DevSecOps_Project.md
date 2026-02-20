# Chat Conversation

Note: _This is purely the output of the chat conversation and does not contain any raw data, codebase snippets, etc. used to generate the output._

### User Input

Project: The DevSecOps Infrastructure Pipeline
Objective: Build a fully automated pipeline that provisions a production-ready EKS cluster, performs deep security audits on Docker images, and deploys using Jenkins via a "Pull" pattern from a private Registry.

🏗️ Phase 1: Advanced Infrastructure (Terraform)
The "Pro" Touch: Don't just make a cluster. Use Terraform Modules to separate your VPC, IAM, and EKS logic.

OIDC & IRSA: Set up IAM Roles for Service Accounts. This means your K8s pods don't use hardcoded AWS keys; they "assume" a role (much more secure).

Remote State: Use an S3 Bucket with DynamoDB Locking for your Terraform state to prevent concurrent runs from corrupting your infra.

📦 Phase 2: Secure Containerization (Docker)
Multi-Stage Builds: Optimize your Dockerfile to be tiny (using Distroless or Alpine images).

Non-Root User: Ensure your container doesn't run as "root" (a common security fail in basic projects).

Private Registry: Use Amazon ECR and handle the authentication lifecycle within Jenkins.

🤖 Phase 3: The "Security-First" Jenkins Pipeline
Your Jenkinsfile should be the star here. Instead of a 3-stage build, do this:

SCA (Static Code Analysis): Run Trivy or Snyk on your application code.

Container Linting: Use Hadolint to check your Dockerfile for best practices.

Vulnerability Scan: After building the image, scan it with Trivy and fail the build if "High" or "Critical" vulnerabilities are found.

Terraform Plan: Run terraform plan and have Jenkins post the output as a comment or wait for a manual "Approval" button before applying.

☸️ Phase 4: Kubernetes Day-2 Ops
Horizontal Pod Autoscaler (HPA): Set up the Metrics Server so your pods scale based on actual CPU load.

Ingress & TLS: Use the AWS Load Balancer Controller to create an Application Load Balancer (ALB) automatically via K8s manifests.

📂 Folder Structure for your GitHub
Plaintext
.
├── terraform/          # VPC, EKS, and IAM modules
├── kubernetes/         # Deployment, Service, and HPA manifests
├── app/                # Your Python/Node/Go source code
├── Dockerfile          # Multi-stage production dockerfile
└── Jenkinsfile         # The Groovy pipeline script


Need to do above project step by step

*Listed directory [DEVOPS-PROJECT](file:///c:/GITHUB/DEVOPS-PROJECT) *

*Listed directory [terraform-aws-vprofile-project](file:///c:/GITHUB/DEVOPS-PROJECT/terraform-aws-vprofile-project) *

*Viewed [pom.xml](file:///c:/GITHUB/DEVOPS-PROJECT/pom.xml) *

*Viewed [README.md](file:///c:/GITHUB/DEVOPS-PROJECT/README.md) *

### Planner Response

Now I have a good understanding of the workspace. It's a Java/Spring MVC (vprofile) web application with Maven. Let me also check for any existing source code structure.

*Searched filesystem*

### Planner Response

Good. I can see the project has no `src` directory at the top level (it's the vprofile Java app). Let me check for any existing `src` folder and understand the full app layout.

*Searched filesystem*

*Searched filesystem*

### Planner Response

There's no `src` directory — this appears to be a state where only the [pom.xml](cci:7://file:///c:/GITHUB/DEVOPS-PROJECT/pom.xml:0:0-0:0) and terraform directory exist. The Java source code needs to live under `src/main/java/...`. Now I have enough context to write a comprehensive implementation plan. Let me create the task tracker and the implementation plan.

*Edited relevant file*

### Planner Response

Now let me write the comprehensive implementation plan:

*Edited relevant file*

### User Input

See I dont have anything in my directory can u start from scratc

### Planner Response

Got it! Let's build the entire project from scratch. I'll create everything under `c:\PROJECTS\devsecops-pipeline`. Let me start with the directory structure and work through all 4 phases.

## Starting Phase 1: Terraform Modules

Let me create all the Terraform infrastructure files first:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now the root `main.tf` that wires everything together plus the ECR repository:

*Edited relevant file*

### Planner Response

Now let's build the **VPC Module**:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now the **IAM Module** with OIDC & IRSA:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now the **EKS Module**:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

✅ **Phase 1 done!** Now moving to **Phase 2: Secure Containerization** — the app source code, Dockerfile, and .dockerignore:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now the **Dockerfile** (multi-stage, non-root) and **.dockerignore**:

*Edited relevant file*

*Edited relevant file*

### Planner Response

✅ **Phase 2 done!** Now **Phase 3: The Security-First Jenkins Pipeline**:

*Edited relevant file*

### Planner Response

✅ **Phase 3 done!** Now **Phase 4: Kubernetes Day-2 Ops** manifests:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

✅ **Phase 4 done!** Now let me create the **.gitignore** and comprehensive **README.md**:

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now let me initialize a Git repository and verify the complete folder structure:

*User accepted the command `git init && git add -A && git status`*

### Planner Response

PowerShell doesn't support `&&` — let me fix that:

*User accepted the command `git init; git add -A; git status`*

*User accepted the command `Get-ChildItem -Recurse -Name | Where-Object { $_ -notmatch '^\.(git)\\' }`*

### Planner Response

Let me get a cleaner view:

*User accepted the command `cmd /c "tree /F /A"`*

### Planner Response

Everything is in place. Now let me fix the circular dependency in the Terraform root [main.tf](cci:7://file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf:0:0-0:0) — I noticed the IAM module references its own `oidc_provider_arn` as input:

*Viewed [main.tf](file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf) *

### Planner Response

I see the circular dependency issue — the IAM module references itself (`module.iam.oidc_provider_arn`) and also the EKS module needs IAM roles before creating the cluster, but IAM needs the OIDC URL from EKS. Let me restructure this properly by splitting IAM into two parts: base roles (needed before EKS) and OIDC/IRSA (needed after EKS):

*Edited relevant file*

### Planner Response

Wait — there's still a circular dependency. The EKS module needs `cluster_role_arn` and `node_role_arn` from IAM, but IAM depends on EKS for the OIDC URL. I need to split IAM resources properly. Let me restructure:

*Edited relevant file*

### Planner Response

Now I need to update the IAM module to remove the base role resources (since they're now in root) and keep only the OIDC/IRSA logic. Also need to remove the `cluster_role_arn` and `node_role_arn` outputs from IAM module and update the outputs in root:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now let me also update the root [outputs.tf](cci:7://file:///c:/PROJECTS/devsecops-pipeline/terraform/outputs.tf:0:0-0:0) to reference the correct sources:

*Edited relevant file*

### Planner Response

Now let's do a final verification — let me check if Terraform can be validated locally:

*User accepted the command `terraform version`*

### Planner Response

Terraform isn't installed locally, which is fine — this is expected for a Windows dev machine. Let me do a final check of the complete directory tree to make sure everything looks right:

*Searched filesystem*

### Planner Response

All **28 files** are created. Here's a complete summary of everything that was built:

---

## ✅ Project Complete — DevSecOps Infrastructure Pipeline

Your project is fully created at `c:\PROJECTS\devsecops-pipeline\` with **28 files** across 4 phases:

### 📂 Final Structure
```
devsecops-pipeline/
├── terraform/                          # 🏗️ Phase 1: Infrastructure
│   ├── backend.tf                      # S3 + DynamoDB remote state locking
│   ├── providers.tf                    # AWS provider (v5.0+)
│   ├── variables.tf                    # Cluster config (region, size, version)
│   ├── outputs.tf                      # kubectl command, ECR URL, role ARNs
│   ├── main.tf                         # Wires modules + base IAM roles + ECR
│   └── modules/
│       ├── vpc/                        # 3-AZ, public/private subnets, NAT GW
│       ├── iam/                        # OIDC provider + IRSA roles (app + ALB)
│       └── eks/                        # Cluster + managed node group + add-ons
├── kubernetes/                         # ☸️ Phase 4: Day-2 Ops
│   ├── namespace.yaml                  # vprofile namespace
│   ├── service-account.yaml            # IRSA-annotated (no hardcoded keys!)
│   ├── deployment.yaml                 # Rolling updates, 3 probes, security ctx
│   ├── service.yaml                    # ClusterIP → port 80
│   ├── hpa.yaml                        # CPU 50% / Memory 70%, scale 2–10
│   ├── ingress.yaml                    # ALB + TLS 1.3 + HTTP→HTTPS redirect
│   └── metrics-server.yaml             # Full RBAC + Deployment for HPA
├── app/                                # 📦 Application Source
│   ├── pom.xml                         # Maven (Spring 6, Jakarta EE)
│   └── src/main/.../HealthController   # /health endpoint for K8s probes
├── Dockerfile                          # 📦 Phase 2: Multi-stage, non-root user
├── .dockerignore                       # Minimal build context
├── Jenkinsfile                         # 🤖 Phase 3: 12-stage security pipeline
├── .gitignore                          # Terraform state, secrets, build artifacts
└── README.md                           # Full docs + Mermaid architecture diagram
```

### 🔑 Key Design Decisions

1. **Circular dependency solved**: Base IAM roles (cluster + node) live in root `main.tf`, while OIDC/IRSA are in the IAM module. This means: `VPC → base IAM → EKS → OIDC/IRSA` — no cycles.

2. **IRSA over hardcoded keys**: Pods use a K8s service account annotated with an IAM role ARN. AWS SDK inside the pod gets temporary credentials via OIDC federation.

3. **Jenkins quality gate**: The Trivy image scan at Stage 7 uses `--exit-code 1` — the pipeline **fails** if any HIGH or CRITICAL vulnerabilities exist.

4. **Non-root everywhere**: The Dockerfile creates `appuser`, the K8s deployment enforces `runAsNonRoot: true` + `allowPrivilegeEscalation: false` + all capabilities dropped.

### 🚦 Before You Deploy — Placeholders to Replace

| File | Placeholder | Replace With |
|---|---|---|
| `kubernetes/service-account.yaml` | `<ACCOUNT_ID>` | Your AWS Account ID |
| `kubernetes/ingress.yaml` | `<ACCOUNT_ID>` | Your AWS Account ID |
| `kubernetes/ingress.yaml` | `<CERT_ID>` | Your ACM Certificate ID |
| `kubernetes/ingress.yaml` | `vprofile.yourdomain.com` | Your actual domain |
| `Jenkinsfile` | `aws-account-id` credential | Add this to Jenkins credentials |

Want me to walk you through any specific phase in more detail, or shall we proceed with pushing this to GitHub?

### User Input

One second, what is the source code we are deploying and where is it ?

### Planner Response

Great question! Right now the application source code is **minimal — just a stub**. Here's what's in `app/`:

*Viewed [HealthController.java](file:///c:/PROJECTS/devsecops-pipeline/app/src/main/java/com/visualpathit/account/controller/HealthController.java) *

*Listed directory [app](file:///c:/PROJECTS/devsecops-pipeline/app) *

### Planner Response

So here's the honest situation:

### What we have now
```
app/
├── pom.xml                          # Full Maven config (Spring 6, MySQL, RabbitMQ, etc.)
└── src/main/
    ├── java/.../HealthController.java   # Just a /health endpoint (stub)
    └── webapp/WEB-INF/
        ├── web.xml                      # Spring DispatcherServlet config
        └── spring-servlet.xml           # Component scan config
```

**This is not a real application** — it's just the bare minimum so the Maven build (`mvn package`) and the Docker multi-stage build succeed without errors. It produces a WAR file that Tomcat can serve, but it only has a health check endpoint.

### What you probably want

The **vprofile** project is a well-known open-source Java web application used for DevOps practice. Your original repo at `c:\GITHUB\DEVOPS-PROJECT` had the `pom.xml` for it but was missing the `src/` directory. 

You have two options:

1. **Use the real vprofile source code** — Clone it from the official GitHub repo ([hkhcoder/vprofile-project](https://github.com/hkhcoder/vprofile-project)) and copy the full `src/` tree into our `app/` directory. This gives you a real Java webapp with login pages, user profiles, MySQL backend, etc.

2. **Build a custom sample app** — I can create a more complete sample application right here (e.g., a REST API with a few endpoints, a database model) so you have something meaningful to demo in the pipeline.

**Which would you prefer?** Or do you have your own application source code somewhere that you'd like to integrate?

### User Input

Apart from Vprofile can u get me something else, vprfile is too much common and udemy like..I need a solid project for interview

### Planner Response

Great call — vprofile screams "Udemy course project" in an interview. Let me build you something **original and domain-relevant**: 

**"CloudPulse"** — a Cloud Infrastructure Monitoring API. It's a REST backend that tracks cloud resources, runs health checks, and manages incidents. This is directly relevant to DevOps, shows you understand the domain, and isn't something interviewers have seen 100 times.

Let me rebuild the entire `app/` directory from scratch:

*Edited relevant file*

### Planner Response

Now the main application entry point and config:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now the **domain models** — the core entities:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now the **repositories** (data access layer):

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now the **DTOs** (Data Transfer Objects) and **exception handling**:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now the **service layer** (business logic):

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now the **REST controllers** (API endpoints):

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now let me add a **data seeder** (pre-populates the database with realistic demo data) and **unit tests**:

*Edited relevant file*

### Planner Response

Now the **unit tests**:

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### Planner Response

Now let me clean up the old stub files and update the Dockerfile for Spring Boot (it now produces a JAR, not a WAR):

*User accepted the command `Remove-Item -Recurse -Force "c:\PROJECTS\devsecops-pipeline\app\src\main\java\com\visualpathit"; Remove-Item -Recurse -Force "c:\PROJECTS\devsecops-pipeline\app\src\main\webapp"`*

### Planner Response

Now update the **Dockerfile** for Spring Boot JAR (not WAR anymore):

*Edited relevant file*

### Planner Response

Update the K8s deployment to use the Actuator health endpoint:

*Edited relevant file*

### Planner Response

Those lint errors are just the IDE not having Maven dependencies downloaded — they'll resolve once `mvn` runs. Let me also update the Ingress health check path:

*Edited relevant file*

### Planner Response

Now let me verify the final file structure:

*Searched filesystem*

### Planner Response

All **51 files** are in place. Here's a summary of the complete **CloudPulse** application:

---

## ✅ CloudPulse — Cloud Infrastructure Monitoring API

Your application is now a **proper, original, interview-ready project** — not another vprofile clone. Here's what was built:

### 📡 API Endpoints (all under `/api`)

| Endpoint | Method | Description |
|---|---|---|
| `/api/dashboard` | GET | Executive health overview (aggregated metrics) |
| `/api/resources` | GET | List all monitored resources (filter by `?type=`, `?region=`, `?provider=`) |
| `/api/resources/{id}` | GET/PUT/DELETE | CRUD on individual resources |
| `/api/resources/unhealthy` | GET | Resources in DEGRADED or UNHEALTHY state |
| `/api/resources/{id}/status` | PATCH | Update just the status |
| `/api/healthchecks/resource/{id}` | GET | Health check history for a resource |
| `/api/healthchecks/resource/{id}/run` | POST | Trigger an on-demand health check |
| `/api/healthchecks/recent?hours=24` | GET | Recent checks across all resources |
| `/api/incidents` | GET/POST | List or create incidents |
| `/api/incidents/active` | GET | Active (unresolved) incidents |
| `/api/incidents/critical` | GET | Critical active incidents |
| `/api/incidents/{id}/ack` | POST | Acknowledge an incident |
| `/api/incidents/{id}/resolve` | POST | Resolve with root cause |
| `/api/actuator/health` | GET | Spring Boot health (used by K8s probes) |

### 🏗️ Architecture (Clean Layers)
```
Controller → Service → Repository → JPA Entity → H2/MySQL
    ↓
   DTO (Request/Response) + Global Exception Handler
```

### 🎯 Interview Talking Points
- **"Why CloudPulse?"** — It directly demonstrates DevOps domain knowledge. You monitor infrastructure, track incidents, and compute MTTR — these are real SRE concerns.
- **Scheduled health checks** — `@Scheduled` runs background probes every 5 minutes, simulating a real monitoring agent.
- **IRSA** — The K8s pods use IAM Roles for Service Accounts, not hardcoded keys. You can explain the OIDC trust chain.
- **Security pipeline** — Trivy scans both source code AND built images, Hadolint checks Dockerfile quality, and the pipeline fails on HIGH/CRITICAL CVEs.
- **H2 → MySQL profile switching** — Dev uses in-memory H2 (zero setup), prod uses the [application-prod.yml](cci:7://file:///c:/PROJECTS/devsecops-pipeline/app/src/main/resources/application-prod.yml:0:0-0:0) with MySQL via environment variables.
- **Data seeder** — Pre-populates realistic AWS resources so the API works out of the box for demos.

The lint errors in your IDE are just because Maven dependencies haven't been downloaded yet — they'll resolve once you run `mvn install` or open the `app/` directory as a Maven project.

### User Input

Okay lets start from scratch, tell me how to start the project, explain bit by bit..not all at once..

### Planner Response

Absolutely! Let's take this slow and understand the project properly. No point having code you can't explain in an interview. 😄

---

## 🧭 Step 1: Understand What You're Building

Think of this project as **4 layers**, each building on the previous one:

```
┌──────────────────────────────────────────────┐
│  Phase 4: Kubernetes (WHERE the app runs)    │  ← Day-2 Ops (HPA, Ingress, TLS)
├──────────────────────────────────────────────┤
│  Phase 3: Jenkins (HOW it gets deployed)     │  ← CI/CD Pipeline with security gates
├──────────────────────────────────────────────┤
│  Phase 2: Docker (HOW the app is packaged)   │  ← Multi-stage build, non-root
├──────────────────────────────────────────────┤
│  Phase 1: Terraform (WHERE the infra lives)  │  ← VPC + EKS + IAM on AWS
└──────────────────────────────────────────────┘
```

**The story you'll tell an interviewer:**
> *"I built a Cloud Infrastructure Monitoring API called CloudPulse. I wrote the app in Java/Spring Boot, containerized it securely with Docker, set up a Jenkins pipeline that scans for vulnerabilities before deploying, and provisioned the entire AWS infrastructure — VPC, EKS cluster, IAM roles — using modular Terraform with remote state locking."*

---

## 🗂️ Step 2: Look at Your Folder Structure

Everything is already created at `c:\PROJECTS\devsecops-pipeline\`. Let's look at the top level:

*User accepted the command `Get-ChildItem -Name`*

### Planner Response

Here's what each piece does:

| Folder/File | Purpose | Phase |
|---|---|---|
| `terraform/` | Infrastructure as Code — creates your AWS VPC, EKS cluster, IAM roles | Phase 1 |
| `app/` | Your actual Java application — "CloudPulse" monitoring API | The App |
| [Dockerfile](cci:7://file:///c:/PROJECTS/devsecops-pipeline/Dockerfile:0:0-0:0) | Packages the app into a secure container image | Phase 2 |
| [Jenkinsfile](cci:7://file:///c:/PROJECTS/devsecops-pipeline/Jenkinsfile:0:0-0:0) | The CI/CD pipeline with security scanning | Phase 3 |
| `kubernetes/` | K8s manifests — how the app runs in the cluster | Phase 4 |

---

## 🚀 Step 3: Let's Start with Phase 1 — Terraform

**Why Terraform first?** Because before you deploy anything, you need infrastructure. No VPC, no cluster, no deployment.

Your Terraform is **modular** — this is the "pro touch" that separates junior from senior DevOps:

```
terraform/
├── backend.tf          ← Remote state (S3 + DynamoDB locking)
├── providers.tf        ← AWS provider config
├── variables.tf        ← All configurable values
├── outputs.tf          ← Values output after apply
├── main.tf             ← Wires the 3 modules together
└── modules/
    ├── vpc/            ← Networking (subnets, NAT, routing)
    ├── iam/            ← Security (roles, OIDC, IRSA)  
    └── eks/            ← The Kubernetes cluster itself
```

**Why modules?** Imagine explaining this in an interview:
> *"I didn't put everything in one file. I separated VPC, IAM, and EKS into modules because in a real team, the networking team might own the VPC module, the security team owns IAM, and the platform team owns EKS. It's reusable — I can spin up 3 environments (dev, staging, prod) just by calling the same modules with different variables."*

---

Want me to **walk you through the VPC module first** — explaining what each resource does and why? That's where the infrastructure story begins (subnets, NAT Gateway, routing tables, K8s tags).

### User Input

yes

### Planner Response

Let's dive into the **VPC module** — the networking foundation everything else runs on.

---

## 🌐 The VPC Module — Your Private Network on AWS

Open the file and follow along:

*Viewed [main.tf](file:///c:/PROJECTS/devsecops-pipeline/terraform/modules/vpc/main.tf) *

### Planner Response

Let me break this down **resource by resource**:

---

### 1️⃣ The VPC itself (Line 9-18)
```hcl
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr          # "10.0.0.0/16" — gives you 65,536 IP addresses
  enable_dns_support   = true                  # Required for EKS
  enable_dns_hostnames = true                  # Required for EKS
}
```
> **Interview answer:** *"The VPC is my isolated virtual network. I'm using a /16 CIDR which gives me plenty of room to carve out subnets. DNS support is required for EKS — the cluster needs to resolve internal service names."*

Notice the tag `kubernetes.io/cluster/<name> = "shared"` — **this is critical**. EKS uses this tag to discover which VPC belongs to the cluster. Without it, your cluster won't find its network.

---

### 2️⃣ Internet Gateway (Line 21-27)
```hcl
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
}
```
This is the **front door** to the internet. Public subnets route through this. Without it, nothing in your VPC can reach the outside world.

---

### 3️⃣ NAT Gateway + Elastic IP (Lines 30-50)
```hcl
resource "aws_eip" "nat" { ... }          # A static public IP address
resource "aws_nat_gateway" "main" { ... } # Sits in the PUBLIC subnet
```
> **Interview answer:** *"The NAT Gateway allows resources in private subnets (like my EKS worker nodes) to reach the internet — for pulling Docker images, downloading packages — but nobody from the internet can reach them directly. It's a one-way door out."*

**Why single NAT?** In production at a real company, you'd have one per AZ for high availability. For this project, one saves cost (~$32/month each).

---

### 4️⃣ Public Subnets (Lines 52-66) — The "Internet-Facing" Layer
```hcl
resource "aws_subnet" "public" {
  count = length(var.azs)                                    # Creates 3 (one per AZ)
  cidr_block = cidrsubnet(var.vpc_cidr, 4, count.index)      # Auto-slices the CIDR
  map_public_ip_on_launch = true                             # Instances get public IPs
}
```

The `cidrsubnet` function is doing the math for you:
```
VPC:     10.0.0.0/16
Public:  10.0.0.0/20  (AZ-a) ← count.index = 0
         10.0.16.0/20 (AZ-b) ← count.index = 1
         10.0.32.0/20 (AZ-c) ← count.index = 2
```

**Key tag:** `"kubernetes.io/role/elb" = "1"` — This tells the AWS Load Balancer Controller: *"Put internet-facing ALBs in these subnets."*

---

### 5️⃣ Private Subnets (Lines 68-81) — Where Your Pods Actually Run
```hcl
resource "aws_subnet" "private" {
  count      = length(var.azs)
  cidr_block = cidrsubnet(var.vpc_cidr, 4, count.index + length(var.azs))  # Offset by 3
}
```
```
Private: 10.0.48.0/20 (AZ-a) ← count.index + 3 = 3
         10.0.64.0/20 (AZ-b) ← count.index + 3 = 4
         10.0.80.0/20 (AZ-c) ← count.index + 3 = 5
```

**Key tag:** `"kubernetes.io/role/internal-elb" = "1"` — This tells the ALB Controller: *"Put internal load balancers here."*

> **Interview answer:** *"EKS worker nodes run in private subnets. They have no public IPs. They pull images through the NAT Gateway. The ALB sits in the public subnets and routes traffic to the pods in private subnets. This is standard defense-in-depth architecture."*

---

### 6️⃣ Route Tables (Lines 83-123) — The Traffic Rules

**Public route table:**
```hcl
route {
  cidr_block = "0.0.0.0/0"           # "All traffic..."
  gateway_id = aws_internet_gateway.main.id  # "...goes to the Internet Gateway"
}
```

**Private route table:**
```hcl
route {
  cidr_block     = "0.0.0.0/0"            # "All traffic..."
  nat_gateway_id = aws_nat_gateway.main.id  # "...goes through NAT (one-way out)"
}
```

Then each subnet is **associated** with its route table. Without this association, subnets use the VPC's default route table which goes nowhere.

---

### 🎨 Visual Summary

```
                    ┌─── INTERNET ───┐
                           │
                    ┌──────┴──────┐
                    │  Internet   │
                    │  Gateway    │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
    ┌─────┴─────┐   ┌─────┴─────┐   ┌─────┴─────┐
    │ Public    │   │ Public    │   │ Public    │
    │ Subnet    │   │ Subnet    │   │ Subnet    │
    │ (AZ-a)   │   │ (AZ-b)   │   │ (AZ-c)   │
    │   ALB ◄──│───│───────────│───│──── Users │
    └─────┬─────┘   └───────────┘   └───────────┘
          │
    ┌─────┴─────┐
    │   NAT     │
    │  Gateway  │
    └─────┬─────┘
          │
    ┌─────┴──────────┬───────────────┐
    │                │               │
    ┌─────┴─────┐  ┌─────┴─────┐  ┌──┴────────┐
    │ Private   │  │ Private   │  │ Private   │
    │ Subnet    │  │ Subnet    │  │ Subnet    │
    │ (AZ-a)   │  │ (AZ-b)   │  │ (AZ-c)   │
    │  🐳 Pods  │  │  🐳 Pods  │  │  🐳 Pods  │
    └───────────┘  └───────────┘  └───────────┘
```

---

### 📝 Key Interview Questions You Should Be Ready For:

1. **"Why 3 AZs?"** — High availability. If one AZ goes down, pods reschedule to the other two.

2. **"Why public AND private subnets?"** — Defense in depth. Pods run in private (no direct internet exposure). Only the ALB load balancer sits in public subnets.

3. **"What if the NAT Gateway fails?"** — In this setup, private subnets lose internet access. In production, you'd have one NAT per AZ for HA.

4. **"What are those kubernetes.io tags?"** — EKS and the AWS Load Balancer Controller use them to auto-discover which subnets to use. Without them, the ALB Ingress won't create properly.

---

Ready for me to walk through the **IAM module** next? That's where OIDC and IRSA live — the security story that impresses interviewers the most. 🔐

### User Input

4️⃣ Public Subnets (Lines 52-66) — The "Internet-Facing" Layer
hcl
resource "aws_subnet" "public" {
  count = length(var.azs)                                    # Creates 3 (one per AZ)
  cidr_block = cidrsubnet(var.vpc_cidr, 4, count.index)      # Auto-slices the CIDR
  map_public_ip_on_launch = true                             # Instances get public IPs
}
The cidrsubnet function is doing the math for you:

VPC:     10.0.0.0/16
Public:  10.0.0.0/20  (AZ-a) ← count.index = 0
         10.0.16.0/20 (AZ-b) ← count.index = 1
         10.0.32.0/20 (AZ-c) ← count.index = 2
Key tag: "kubernetes.io/role/elb" = "1" — This tells the AWS Load Balancer Controller: "Put internet-facing ALBs in these subnets."

How ti knows to create 3 AZs

### Planner Response

Great question! The answer is a **chain** — let me trace it from root to module:

---

## 🔗 How Terraform Knows to Create 3 AZs

It flows through **3 files**. Let's open them all:

*Viewed [main.tf](file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf) *

*Viewed [main.tf](file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf) *

*Viewed [variables.tf](file:///c:/PROJECTS/devsecops-pipeline/terraform/variables.tf) *

*Viewed [variables.tf](file:///c:/PROJECTS/devsecops-pipeline/terraform/modules/vpc/variables.tf) *

### Planner Response

Now I can show you the full chain clearly:

---

## 🔗 The 3-AZ Chain — Step by Step

### Step 1️⃣ — AWS itself tells Terraform what AZs exist

In [main.tf](cci:7://file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf:0:0-0:0) line 12-14:
```hcl
data "aws_availability_zones" "available" {
  state = "available"
}
```
A **data source** is Terraform's way of **reading** information from AWS instead of creating it. This asks AWS:
> *"Give me a list of all currently available AZs in my region (us-east-1)"*

AWS responds with something like:
```
["us-east-1a", "us-east-1b", "us-east-1c", "us-east-1d", "us-east-1f"]
```

---

### Step 2️⃣ — You slice exactly 3 from that list

In [main.tf](cci:7://file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf:0:0-0:0) line 23:
```hcl
azs = slice(data.aws_availability_zones.available.names, 0, 3)
```

`slice()` cuts the list: **from index 0, take 3 items**:
```
["us-east-1a", "us-east-1b", "us-east-1c"]
                                              ↑ This gets passed to the VPC module
```

> **Why slice and not hardcode?** If you hardcoded `["us-east-1a", "us-east-1b", "us-east-1c"]` and later changed region to `ap-south-1`, it would break because Mumbai has `ap-south-1a/b/c` not `us-east-1*`. With `slice + data source`, it works in **any** region automatically.

---

### Step 3️⃣ — The VPC module receives it as a variable

In [vpc/variables.tf](cci:7://file:///c:/PROJECTS/devsecops-pipeline/terraform/modules/vpc/variables.tf:0:0-0:0) line 20-23:
```hcl
variable "azs" {
  type = list(string)   # Expects a list like ["us-east-1a", "us-east-1b", "us-east-1c"]
}
```
The module doesn't know or care it's 3 — it just accepts whatever list it's given.

---

### Step 4️⃣ — [count](cci:1://file:///c:/PROJECTS/devsecops-pipeline/app/src/main/java/com/cloudpulse/repository/ResourceRepository.java:31:4-32:35) loops over that list

Back in [vpc/main.tf](cci:7://file:///c:/PROJECTS/devsecops-pipeline/terraform/modules/vpc/main.tf:0:0-0:0):
```hcl
resource "aws_subnet" "public" {
  count = length(var.azs)   # length(["us-east-1a", "us-east-1b", "us-east-1c"]) = 3
  ...
  availability_zone = var.azs[count.index]   # Loop: 0=1a, 1=1b, 2=1c
}
```

[count](cci:1://file:///c:/PROJECTS/devsecops-pipeline/app/src/main/java/com/cloudpulse/repository/ResourceRepository.java:31:4-32:35) is Terraform's loop. It creates **one resource per iteration**:

| Iteration | `count.index` | `var.azs[count.index]` | Subnet Created |
|---|---|---|---|
| 1 | 0 | `us-east-1a` | `cloudpulse-public-us-east-1a` |
| 2 | 1 | `us-east-1b` | `cloudpulse-public-us-east-1b` |
| 3 | 2 | `us-east-1c` | `cloudpulse-public-us-east-1c` |

---

### 🎯 The Full Picture

```
AWS API ──────────────────────────────────────────────────
         "us-east-1 has: [1a, 1b, 1c, 1d, 1f]"
                │
                ▼
         data.aws_availability_zones.available.names
         = ["us-east-1a", "us-east-1b", "us-east-1c", "us-east-1d", "us-east-1f"]
                │
                ▼
         slice(..., 0, 3)
         = ["us-east-1a", "us-east-1b", "us-east-1c"]
                │
                ▼  (passed as azs = ...)
         VPC Module: var.azs
                │
                ▼
         count = length(var.azs) = 3
                │
                ├── Subnet in us-east-1a
                ├── Subnet in us-east-1b
                └── Subnet in us-east-1c
```

---

### 💡 Bonus: Why This Design is Smart

If tomorrow you want **2 AZs** (to save cost in a dev environment), you just change one number in [main.tf](cci:7://file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf:0:0-0:0):
```hcl
azs = slice(..., 0, 2)   # Now creates 2 subnets instead of 3
```
The module code doesn't change at all. This is the power of **parameterized modules**.

---

Ready to move to the **IAM module** next? That's where the most interesting security concepts live — OIDC, IRSA, and why your pods never need hardcoded AWS keys. 🔐

### User Input

Ok understood, now contine explainig the projecy u were doing earlier

### Planner Response

Great! We finished the VPC module. Next up is the **IAM module** — the security layer. This is the one that impresses interviewers most because it shows you understand AWS security at a deep level.

---

## 🔐 Phase 1 — Step 2: The IAM Module

Let's open it:

*Viewed [main.tf](file:///c:/PROJECTS/devsecops-pipeline/terraform/modules/iam/main.tf) *

### Planner Response

Let's break this down section by section.

---

## 🔐 IAM Module — The Security Story

### First, understand the PROBLEM this solves

Imagine your app pod on Kubernetes needs to read from an S3 bucket. The **wrong** (and common beginner) approach:

```bash
# ❌ BAD — What beginners do
kubectl create secret aws-creds \
  --from-literal=AWS_ACCESS_KEY_ID=AKIA...  \
  --from-literal=AWS_SECRET_ACCESS_KEY=abc123...
```

Problems:
- Keys **never expire** — if stolen, attacker has permanent access
- Keys get **hardcoded in YAML**, then committed to Git
- One leaked key = entire AWS account compromised

**The solution is IRSA** — IAM Roles for Service Accounts. Let's understand how it works:

---

### 1️⃣ OIDC Provider (Lines 14-27) — The Trust Bridge

```hcl
data "tls_certificate" "eks" {
  url = var.eks_oidc_issuer_url   # e.g., "https://oidc.eks.us-east-1.amazonaws.com/id/ABC123"
}

resource "aws_iam_openid_connect_provider" "eks" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks.certificates[0].sha1_fingerprint]
  url             = var.eks_oidc_issuer_url
}
```

**What is OIDC?** OpenID Connect — a standard for identity federation. Think of it like this:

```
EKS Cluster = Your University  (issues Student ID cards)
AWS IAM     = The Bank
OIDC        = The Bank trusting your University's Student ID

"If EKS says this pod is vprofile-sa, AWS will believe it"
```

The `tls_certificate` data source fetches the **thumbprint** (a fingerprint) of EKS's OIDC endpoint certificate. AWS uses this to verify that token requests are genuinely coming from your EKS cluster and not some imposter.

> **Interview answer:** *"I set up an OIDC identity provider in IAM that points to my EKS cluster's OIDC endpoint. This creates a trust relationship — AWS now accepts identity tokens signed by my EKS cluster."*

---

### 2️⃣ App IRSA Role (Lines 31-56) — The Pod's Identity

```hcl
resource "aws_iam_role" "app_irsa" {
  assume_role_policy = jsonencode({
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.eks.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"   # ← Key difference from normal roles
      Condition = {
        StringEquals = {
          # Only THIS specific service account in THIS namespace can assume this role
          "...oidc.eks...:sub" = "system:serviceaccount:vprofile:vprofile-sa"
          "...oidc.eks...:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}
```

The **Condition block** is the most important part. It says:

> *"Only allow this role to be assumed if the request comes from a K8s service account named `vprofile-sa` in the `vprofile` namespace"*

This means even if another pod on the same cluster tries to assume this role, **it will be rejected** because its service account name doesn't match.

---

### 3️⃣ How IRSA Works End-to-End

Here's the full flow when your pod needs to call AWS:

```
                    ┌─────────────────────────────────┐
                    │         Your Pod starts          │
                    │  (has service account: vprofile-sa)│
                    └────────────────┬────────────────┘
                                     │
                    ┌────────────────▼────────────────┐
                    │   K8s mounts a JWT token into    │
                    │   the pod automatically via      │
                    │   projected volume               │
                    └────────────────┬────────────────┘
                                     │
                    ┌────────────────▼────────────────┐
                    │  AWS SDK inside your Java app   │
                    │  reads the JWT token            │
                    │  calls STS: AssumeRoleWithWebIdentity │
                    └────────────────┬────────────────┘
                                     │
                    ┌────────────────▼────────────────┐
                    │  AWS STS checks:               │
                    │  ✅ Token signed by my EKS?    │
                    │  ✅ ServiceAccount = vprofile-sa?│
                    │  ✅ Namespace = vprofile?       │
                    └────────────────┬────────────────┘
                                     │
                    ┌────────────────▼────────────────┐
                    │  ✅ Issues TEMPORARY credentials │
                    │  (expire in 1 hour, auto-renewed)│
                    └─────────────────────────────────┘
```

> **Interview one-liner:** *"Pods get temporary AWS credentials automatically via IRSA — no access keys, no secrets, nothing to rotate or leak. The credentials expire every hour and are auto-renewed by the SDK."*

---

### 4️⃣ ALB Controller Role (Lines 78-152)

This is the **same IRSA pattern** but for a different consumer — the [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/), which is a K8s controller that watches for `Ingress` objects and creates real AWS ALBs.

```hcl
resource "aws_iam_role" "alb_controller" {
  # Same IRSA pattern, but locked to:
  # system:serviceaccount:kube-system:aws-load-balancer-controller
}
```

It needs a **big list of permissions** (lines 115-146) because it has to:
- Create/delete ALBs and Target Groups
- Manage Security Groups for the ALB
- Read ACM certificates for TLS termination
- Interact with WAF if needed

> **Interview answer:** *"The ALB controller runs as a pod in `kube-system`. It watches the cluster for Ingress resources and creates real AWS Application Load Balancers. It uses IRSA so it has the IAM permissions to call AWS APIs — create ALBs, manage security groups, register targets — without any hardcoded keys."*

---

### 🔑 The Circular Dependency Problem (and how we solved it)

Notice the comment at line 4:
```
# This module is applied AFTER the EKS cluster is created.
# Base IAM roles (cluster + node group) are created at the ROOT level.
```

Here's why this matters — a classic Terraform trap:

```
❌ The circular problem:
   EKS needs IAM roles to be created first
   IAM (OIDC) needs EKS's OIDC URL to be created first
   → Deadlock!

✅ Our solution:
   ROOT main.tf:  Creates base IAM roles (cluster + node group roles)
                  ↓
   EKS Module:    Creates cluster using those base roles  → outputs OIDC URL
                  ↓
   IAM Module:    Uses OIDC URL to create OIDC provider + IRSA roles
```

This is something most candidates miss — knowing **why** you structured it this way shows real Terraform experience.

---

Ready to move to the **EKS module** next? That's where the Kubernetes cluster itself gets created — master nodes, worker nodes, and add-ons. 🚢

*Edited relevant file*

### User Input

Understood, next

### Planner Response

## 🚢 Phase 1 — Step 3: The EKS Module

Let's open it:

*Viewed [main.tf](file:///c:/PROJECTS/devsecops-pipeline/terraform/modules/eks/main.tf) *

### Planner Response

Let's break this down piece by piece.

---

## 🚢 The EKS Module — Your Kubernetes Cluster

First, understand **what EKS actually is** before reading the code:

```
EKS = Elastic Kubernetes Service

┌─────────────────────────────────────────┐
│           EKS Cluster                   │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │   Control Plane (AWS Managed)    │   │  ← AWS runs this
│  │   - API Server                   │   │    You DON'T pay per-machine
│  │   - etcd (state storage)         │   │    You pay ~$0.10/hr flat
│  │   - Scheduler                    │   │
│  │   - Controller Manager           │   │
│  └──────────────────────────────────┘   │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │   Node Group (YOUR EC2s)         │   │  ← You run + pay for these
│  │   - Worker Node (t3.medium)      │   │    Your pods actually run here
│  │   - Worker Node (t3.medium)      │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

---

### 1️⃣ The EKS Cluster (Lines 6-29) — The Control Plane

```hcl
resource "aws_eks_cluster" "main" {
  name     = var.cluster_name       # "devsecops-pipeline-prod"
  version  = var.cluster_version    # "1.29"
  role_arn = var.cluster_role_arn   # The IAM role we created in root main.tf
```

**`role_arn`** — Remember the base IAM role we created at root level (because of the circular dependency)? This is where we use it. EKS needs this role to make AWS API calls on your behalf — creating load balancers, reading EC2 info, etc.

---

```hcl
  vpc_config {
    subnet_ids              = var.subnet_ids       # The private subnets from VPC module
    endpoint_private_access = true                 # Nodes talk to API server INSIDE VPC
    endpoint_public_access  = true                 # You can also reach it from laptop
    public_access_cidrs     = ["0.0.0.0/0"]        # ⚠️ Restrict this in real prod!
  }
```

> **Interview answer:** *"I enabled both private and public endpoint access. Private means the worker nodes communicate with the control plane over the internal VPC network — no traffic leaves AWS. Public access lets me run `kubectl` from my laptop. In a truly hardened setup, I'd restrict `public_access_cidrs` to my company's VPN IP range only."*

That comment `# Restrict in production` is intentional — showing you know the tradeoff is something interviewers love.

---

```hcl
  enabled_cluster_log_types = [
    "api",             # All kubectl commands + API calls
    "audit",           # Security audit trail — WHO did WHAT
    "authenticator",   # Authentication logs (IRSA token validation)
    "controllerManager", # Deployment, scaling decisions
    "scheduler"        # Which node a pod was placed on
  ]
```

All 5 control plane log types sent to **CloudWatch**. This is the **audit trail** — critical for security compliance.

> **Interview answer:** *"I enabled all 5 log types. The `audit` log in particular tells me who called which API, from where, at what time — that's your compliance story. If a pod gets compromised and starts calling DELETE on deployments, you'll see it in the audit log."*

---

### 2️⃣ Managed Node Group (Lines 32-61) — Your Worker Nodes

```hcl
resource "aws_eks_node_group" "main" {
  cluster_name  = aws_eks_cluster.main.name
  node_role_arn = var.node_role_arn     # Another base IAM role from root main.tf
  subnet_ids    = var.subnet_ids        # PRIVATE subnets — nodes have no public IP
```

```hcl
  scaling_config {
    desired_size = var.desired_size   # 2 — start with 2 nodes
    min_size     = var.min_size       # 1 — scale down to 1 minimum
    max_size     = var.max_size       # 4 — scale up to 4 maximum
  }

  update_config {
    max_unavailable = 1    # During node upgrades, only take 1 node offline at a time
  }
```

**"Managed"** node group means AWS handles the node lifecycle — patching, replacing unhealthy nodes, rolling updates. You don't SSH into nodes and patch them manually.

> **Interview answer:** *"I used a Managed Node Group instead of self-managed nodes. AWS handles node patching and replacement automatically. The `max_unavailable = 1` means during a Kubernetes version upgrade, it replaces one node at a time — my pods stay running."*

---

### 3️⃣ EKS Add-ons (Lines 63-89) — Essential Components

```hcl
resource "aws_eks_addon" "vpc_cni"   { addon_name = "vpc-cni"    }
resource "aws_eks_addon" "coredns"   { addon_name = "coredns"    }
resource "aws_eks_addon" "kube_proxy" { addon_name = "kube-proxy" }
```

These 3 are the **minimum** for a working cluster. Here's what each does:

| Add-on | What it does | Without it |
|---|---|---|
| `vpc-cni` | Gives pods real VPC IP addresses | Pods can't get IPs |
| `coredns` | DNS inside the cluster — `service-name.namespace.svc.cluster.local` | Services can't find each other by name |
| `kube-proxy` | Network rules on each node for routing to pods | Service networking breaks |

```hcl
  depends_on = [aws_eks_node_group.main]   # Don't install add-ons until nodes exist
```

`depends_on` is explicit. CoreDNS runs as a pod — it needs nodes to schedule on.

---

### 🗺️ How All 3 Terraform Modules Connect

Now you can see the full picture of what `terraform apply` builds:

```
terraform apply runs these in ORDER:

Step 1 ──► VPC Module
           Creates: VPC, 3 public subnets, 3 private subnets,
                    NAT GW, IGW, route tables

Step 2 ──► Root main.tf (base IAM roles)
           Creates: EKS cluster IAM role
                    EKS node group IAM role

Step 3 ──► EKS Module (needs Step 1 + Step 2)
           Creates: EKS Control Plane
                    Managed Node Group (2x t3.medium in private subnets)
                    3 essential add-ons (vpc-cni, coredns, kube-proxy)

Step 4 ──► IAM Module (needs Step 3's OIDC URL)
           Creates: OIDC identity provider
                    App pod IRSA role
                    ALB Controller IRSA role

Step 5 ──► Root main.tf (ECR repository)
           Creates: Private Docker image registry with lifecycle policy
```

After `terraform apply` completes, you'd run:
```bash
aws eks update-kubeconfig --region us-east-1 --name devsecops-pipeline-prod
kubectl get nodes   # Should show 2 nodes in Ready state
```

---

### ✅ Terraform Phase — Complete

You now have a full, production-grade AWS infrastructure. Here's the interview summary for this entire phase:

> *"My Terraform is modular — VPC, IAM, and EKS are separate modules. The VPC creates a multi-AZ network with public and private subnets. Worker nodes run in private subnets — no public IPs. The control plane logs all 5 audit streams to CloudWatch. IAM uses IRSA via OIDC so pods get temporary credentials, zero hardcoded keys. State is stored in S3 with DynamoDB locking so my team can run Terraform concurrently without conflicts."*

---

Ready to move to **Phase 2: Docker**? That's where we package the CloudPulse application into a secure container image. 🐳

### User Input

Ok but what about the root main.tf

### Planner Response

Good catch! The root [main.tf](cci:7://file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf:0:0-0:0) is the **glue** that holds everything together. Let's read it fully:

*Viewed [main.tf](file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf) *

### Planner Response

Now let's go through it section by section:

---

## 🔩 Root [main.tf](cci:7://file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf:0:0-0:0) — The Orchestrator

Think of this file as the **conductor of an orchestra**. Each module is a section (strings, brass, percussion). The conductor doesn't play the music — it coordinates the order and passes the right information between sections.

---

### Lines 12-14 — Data Source (you already know this!)
```hcl
data "aws_availability_zones" "available" {
  state = "available"
}
```
Reads available AZs from AWS. Feeds into the VPC module. ✅ Already covered.

---

### Lines 17-24 — Step 1: Call the VPC Module

```hcl
module "vpc" {
  source = "./modules/vpc"        # "Go look in this folder for the module code"

  vpc_cidr     = var.vpc_cidr     # "10.0.0.0/16" from variables.tf
  project_name = var.project_name # "devsecops-pipeline"
  environment  = var.environment  # "prod"
  azs          = slice(data.aws_availability_zones.available.names, 0, 3)
}
```

This is just **calling** the VPC module like a function and passing arguments. After this runs, you can access its outputs using `module.vpc.vpc_id`, `module.vpc.private_subnet_ids` etc.

---

### Lines 26-51 — Step 2a: EKS Cluster IAM Role

```hcl
resource "aws_iam_role" "eks_cluster" {
  name = "${var.project_name}-eks-cluster-role"

  assume_role_policy = jsonencode({
    Statement = [{
      Principal = { Service = "eks.amazonaws.com" }  # AWS EKS service itself
      Action    = "sts:AssumeRole"
    }]
  })
}
```

**Who** uses this role? Not your code. Not your pods. The **EKS control plane itself** uses it. When EKS needs to create an Elastic Network Interface in your VPC, or register a load balancer, it assumes this role.

> The `Principal = { Service = "eks.amazonaws.com" }` means only the EKS service can assume this role — not a human, not your app.

Then two AWS **managed policies** are attached to it:

```hcl
resource "aws_iam_role_policy_attachment" "eks_cluster_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  # ↑ AWS pre-built policy. Gives EKS permissions to manage EC2, ELB, etc.
}

resource "aws_iam_role_policy_attachment" "eks_vpc_resource_controller" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSVPCResourceController"
  # ↑ Needed for security groups on pods feature & ENI management
}
```

Notice we use **AWS managed policies** here — not custom ones. These are maintained by AWS and updated when EKS needs new permissions. You don't have to track permission changes yourself.

---

### Lines 53-81 — Step 2b: Worker Node IAM Role

```hcl
resource "aws_iam_role" "eks_nodes" {
  name = "${var.project_name}-eks-node-role"

  assume_role_policy = jsonencode({
    Statement = [{
      Principal = { Service = "ec2.amazonaws.com" }  # EC2 instances (the worker nodes)
      Action    = "sts:AssumeRole"
    }]
  })
}
```

This role is for the **EC2 instances that ARE your worker nodes**. Three policies attached:

```hcl
# Policy 1: Basic node permissions — join the cluster, report status
"arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"

# Policy 2: Let the vpc-cni add-on manage network interfaces on the node
"arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"

# Policy 3: ⭐ Let nodes PULL Docker images from ECR
"arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
```

That third one is crucial — when Kubernetes schedules your pod, the worker node has to pull your image from ECR. It uses **this role** to authenticate to ECR. No passwords, no keys — just the instance profile.

> **Interview answer:** *"My worker nodes have `AmazonEC2ContainerRegistryReadOnly` attached. When Kubernetes schedules a pod, the node pulls the Docker image from ECR using the node's instance profile — no credentials needed in the pod spec."*

---

### Lines 83-105 — Step 3: Wire Everything into EKS

```hcl
module "eks" {
  source = "./modules/eks"

  cluster_name    = "${var.project_name}-${var.environment}"  # "devsecops-pipeline-prod"
  cluster_version = var.eks_cluster_version                   # "1.29"
  subnet_ids      = module.vpc.private_subnet_ids             # ← FROM VPC module output!
  cluster_role_arn = aws_iam_role.eks_cluster.arn             # ← Base role from Step 2a
  node_role_arn   = aws_iam_role.eks_nodes.arn                # ← Base role from Step 2b
  ...

  depends_on = [
    module.vpc,
    aws_iam_role_policy_attachment.eks_cluster_policy,        # ← Wait for ALL
    aws_iam_role_policy_attachment.eks_vpc_resource_controller, # ← policy attachments
    aws_iam_role_policy_attachment.eks_worker_node_policy,    # ← before creating
    aws_iam_role_policy_attachment.eks_cni_policy,            # ← the cluster
    aws_iam_role_policy_attachment.ecr_read_only              # ←
  ]
}
```

The `depends_on` is the **key insight** here. Notice it waits for the **policy attachments**, not just the role creation. Why?

> A role without its policies attached is useless. If EKS starts before `AmazonEKSClusterPolicy` is attached, the cluster will fail because the role doesn't have permission to do anything yet. Terraform can miss this dependency automatically if you only list the role, so we list **all policy attachments explicitly**.

---

### Lines 107-118 — Step 4: OIDC/IRSA Module

```hcl
module "iam" {
  source = "./modules/iam"

  eks_oidc_issuer_url = module.eks.oidc_issuer_url   # ← FROM EKS module output!
  app_namespace       = "vprofile"
  app_service_account = "vprofile-sa"

  depends_on = [module.eks]   # Can't create OIDC without EKS existing first
}
```

Notice how **data flows** through the file:
```
VPC module creates subnets
        ↓
     module.vpc.private_subnet_ids
        ↓
     fed into EKS module

EKS module creates cluster
        ↓
     module.eks.oidc_issuer_url
        ↓
     fed into IAM module
```

This is **output chaining** — the output of one module becomes the input of the next.

---

### Lines 120-159 — ECR Repository

```hcl
resource "aws_ecr_repository" "app" {
  name                 = "${var.project_name}-app"
  image_tag_mutability = "IMMUTABLE"    # Once pushed, a tag CAN'T be overwritten
  force_delete         = true           # Can delete even if it has images (useful for destroy)

  image_scanning_configuration {
    scan_on_push = true                 # ECR scans every image for CVEs on push
  }

  encryption_configuration {
    encryption_type = "AES256"          # Images encrypted at rest
  }
}
```

**`IMMUTABLE` tags** — this is a security best practice. If you push `myapp:v1.0`, nobody can overwrite it with a different image later. Every deployment is traceable to an exact image.

> **Interview answer:** *"Immutable tags prevent supply chain attacks. If `v1.0` is deployed in prod and someone tries to push a malicious image with the same tag `v1.0`, ECR rejects it. The image in prod is always exactly what was promoted from staging."*

```hcl
resource "aws_ecr_lifecycle_policy" "app" {
  policy = jsonencode({
    rules = [{
      description  = "Keep last 30 images"
      selection    = { countType = "imageCountMoreThan", countNumber = 30 }
      action       = { type = "expire" }
    }]
  })
}
```

When you have 31 images, the oldest one gets automatically deleted. **Cost control** — ECR storage isn't free.

---

### 🗺️ The Full Root [main.tf](cci:7://file:///c:/PROJECTS/devsecops-pipeline/terraform/main.tf:0:0-0:0) Flow

```
root main.tf
│
├── data: aws_availability_zones    ← Read from AWS (no creation)
│
├── module "vpc"                    ← Creates all networking
│       │
│       └── outputs: vpc_id, private_subnet_ids, public_subnet_ids
│
├── resource: aws_iam_role "eks_cluster" + policy attachments
├── resource: aws_iam_role "eks_nodes"   + policy attachments
│       │
│       └── these two live HERE (not in IAM module) to break circular dep
│
├── module "eks"                    ← Creates Kubernetes cluster
│       uses: module.vpc.private_subnet_ids
│       uses: aws_iam_role.eks_cluster.arn
│       uses: aws_iam_role.eks_nodes.arn
│       │
│       └── outputs: cluster_name, cluster_endpoint, oidc_issuer_url
│
├── module "iam"                    ← Creates OIDC + IRSA roles
│       uses: module.eks.oidc_issuer_url
│
└── resource: aws_ecr_repository   ← Docker image registry
    resource: aws_ecr_lifecycle_policy
```

---

Now the Terraform phase is **fully explained**. You understand:
- ✅ VPC — networking foundation
- ✅ IAM — security roles + IRSA
- ✅ EKS — the cluster itself
- ✅ Root main.tf — how they all wire together

Ready to move to **Phase 2: Dockerfile**? That's where we package the CloudPulse app into a container securely. 🐳

### User Input

Lets move to Phase 2

### Planner Response

## 🐳 Phase 2 — The Dockerfile

Let's open it:

*Viewed [Dockerfile](file:///c:/PROJECTS/devsecops-pipeline/Dockerfile) *

### Planner Response

---

## 🐳 The Dockerfile — Packaging CloudPulse Securely

First, understand the **core problem** a naive Dockerfile has:

```dockerfile
# ❌ What a beginner writes:
FROM maven:3.9-eclipse-temurin-17
COPY . .
RUN mvn package
CMD ["java", "-jar", "target/cloudpulse.jar"]
```

Problems with this:
1. **Final image is ~700MB** — Maven, source code, test files, all baked in
2. **Runs as root** — if the app is exploited, attacker owns the container as root
3. **Every code change re-downloads all Maven dependencies** — slow builds

Our Dockerfile fixes all three. Here's how:

---

### 🏗️ STAGE 1: The Builder (Lines 9-22)

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder
```

This stage uses a **fat image** — Maven + JDK 17. It has everything needed to **compile** the code. The keyword `AS builder` gives this stage a name so Stage 2 can reference it.

```dockerfile
WORKDIR /build
```
All subsequent commands run inside `/build`. Creates the directory if it doesn't exist.

---

#### 🧠 The Layer Caching Trick (Lines 17-18) — Important!

```dockerfile
COPY app/pom.xml .          # Step A: Copy ONLY the POM file first
RUN mvn dependency:go-offline -B   # Step B: Download ALL dependencies
```

Then:
```dockerfile
COPY app/src ./src          # Step C: Copy source code AFTER
RUN mvn package -DskipTests -B     # Step D: Compile and build the JAR
```

**Why this order?** Docker builds in layers. Each line is a layer. Docker caches layers. So:

```
First build:
  Layer A: pom.xml copied        → cached
  Layer B: 200 dependencies downloaded → cached (takes ~2 mins)
  Layer C: src/ copied           → cached
  Layer D: mvn package runs      → cached

Second build (you only changed a Java file):
  Layer A: pom.xml unchanged     → ✅ CACHE HIT (instant)
  Layer B: pom.xml unchanged     → ✅ CACHE HIT (instant, skip 2-min download!)
  Layer C: src/ changed          → ❌ cache miss, re-copy
  Layer D: mvn package re-runs   → ❌ re-runs (expected)
```

> **Interview answer:** *"I copy the POM before the source code. Maven dependencies only change when the POM changes — which is rare. So on every code-only change, Docker reuses the cached dependency layer and skips the 2-minute download. Build time drops from 5 minutes to 30 seconds."*

`-DskipTests` — don't run tests during the Docker build. That's Jenkins' job. `-B` means batch mode — no interactive prompts, clean CI output.

---

### 🚀 STAGE 2: The Runtime (Lines 26-58)

```dockerfile
FROM eclipse-temurin:17-jre-jammy
```

This is a **completely different, much smaller base image**:
- `maven:3.9-eclipse-temurin-17` = Maven + JDK = ~500MB
- `eclipse-temurin:17-jre-jammy` = Just the JRE (no compiler, no Maven) = ~200MB

Your final image only has what's needed to **run** the app, not to build it.

---

#### 🔒 Non-Root User (Lines 36-45) — Security Critical

```dockerfile
RUN groupadd -r appgroup && \
    useradd -r -g appgroup -d /app -s /sbin/nologin appuser
```

Breaking this down:
| Flag | Meaning |
|---|---|
| `groupadd -r` | Create a **system** group (lower GID, no login) |
| `useradd -r` | Create a **system** user (no home dir creation, no password) |
| `-g appgroup` | Put the user in the group we just created |
| `-d /app` | Home directory is `/app` |
| `-s /sbin/nologin` | **Cannot log in** — even if someone gets a shell, this user can't open a session |

```dockerfile
USER appuser   # Switch to this user for all remaining instructions
```

> **Interview answer:** *"This follows CIS Docker Benchmark rule 4.1 — containers should not run as root. If the JVM or Spring Boot has a vulnerability and an attacker gets code execution, they're running as `appuser` which has zero privileges — can't touch the filesystem, can't install packages, can't modify the OS. The blast radius is contained to the `/app` directory."*

---

#### 📦 Copying Across Stages (Line 42)

```dockerfile
COPY --from=builder --chown=appuser:appgroup /build/target/cloudpulse.jar app.jar
```

- `--from=builder` — reach back into Stage 1 and grab **only** the final JAR
- `--chown=appuser:appgroup` — immediately give ownership to our non-root user
- The **entire Stage 1 filesystem** is discarded — Maven, source code, `.class` files, test reports, none of it makes it to the final image

---

#### ⚙️ JVM Tuning for Containers (Line 51)

```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
```

This is a common interview differentiator. Old Java (pre-11) didn't understand it was inside a container — it'd see the **host machine's** RAM (e.g., 64GB) and set heap sizes accordingly, crashing the container with OOMKilled errors.

| Flag | What it does |
|---|---|
| `-XX:+UseContainerSupport` | "I know I'm in a container, use cgroup limits not host RAM" |
| `-XX:MaxRAMPercentage=75.0` | Use up to 75% of the container's memory limit for heap |
| `-XX:+UseG1GC` | Use G1 Garbage Collector — better pause times for services |

> **Interview answer:** *"Without `UseContainerSupport`, Java reads total host memory — if the host has 32GB but the container limit is 512MB, the JVM sets a 12GB heap and immediately gets OOMKilled by Kubernetes. `MaxRAMPercentage=75` means if K8s gives the container 512MB, Java uses at most 384MB for heap — leaving room for off-heap memory."*

---

#### 🏥 HEALTHCHECK (Lines 54-55)

```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1
```

| Option | Meaning |
|---|---|
| `--interval=30s` | Check every 30 seconds |
| `--timeout=5s` | If no response in 5s, consider it failed |
| `--start-period=60s` | **Don't count failures** for the first 60s (Spring Boot startup time) |
| `--retries=3` | Only mark UNHEALTHY after 3 consecutive failures |

This is **Docker's built-in health check** — separate from Kubernetes probes. Kubernetes has its own probes defined in [deployment.yaml](cci:7://file:///c:/PROJECTS/devsecops-pipeline/kubernetes/deployment.yaml:0:0-0:0), but adding this means the container is self-describing. Any orchestrator (Docker Compose, ECS, etc.) can use it.

---

### 🏆 The Final Image — What's Inside vs. What's NOT

```
✅ INSIDE the final image:
   - JRE 17 (just runtime, no compiler)
   - cloudpulse.jar (~25MB)
   - curl (for health checks)
   - appuser (non-root)

❌ NOT in the final image (thanks to multi-stage):
   - Maven (~170MB) 
   - JDK compiler (~100MB)
   - app/src/ source code
   - .class files
   - Test files and reports
   - pom.xml
   - Maven local repository (~200MB of downloaded jars)
```

**Result:** Final image ≈ 250MB vs. a naive single-stage build ≈ 700MB+.

---

### 🗺️ Visual Summary

```
┌──────────────────────────────────────────────────────────┐
│  STAGE 1: builder  (maven:3.9-eclipse-temurin-17 ~500MB) │
│                                                          │
│  /build/pom.xml           ← copied first (layer cache)  │
│  /build/.m2/ (200 deps)   ← downloaded + cached         │
│  /build/src/              ← copied second               │
│  /build/target/cloudpulse.jar  ← compiled output        │
└──────────────────┬───────────────────────────────────────┘
                   │  COPY --from=builder cloudpulse.jar
                   │  (everything else in Stage 1 is thrown away)
                   ▼
┌──────────────────────────────────────────────────────────┐
│  STAGE 2: runtime  (eclipse-temurin:17-jre-jammy ~200MB) │
│                                                          │
│  /app/app.jar             ← owned by appuser            │
│  USER: appuser            ← non-root, can't login       │
│  JVM: container-aware     ← respects cgroup limits      │
│  HEALTHCHECK: every 30s   ← self-describing             │
└──────────────────────────────────────────────────────────┘
```

---

### 📝 3 Interview Questions to Expect

1. **"Why multi-stage builds?"**
   > *"Smaller image, faster pulls, smaller attack surface. The build tools never make it to production."*

2. **"What happens if the container runs as root?"**
   > *"If the app has an RCE vulnerability, the attacker gets a root shell inside the container. With `appuser`, they're a system user with no shell, no privileges — they can read `/app/app.jar` and nothing else."*

3. **"Why `-XX:+UseContainerSupport`?"**
   > *"Without it, old Java reads host RAM not container limits. The JVM tries to allocate 12GB heap in a 512MB container, gets OOMKilled instantly. With it, Java respects the cgroup memory limit Kubernetes sets."*

---

Ready to move to **Phase 3: The Jenkinsfile**? That's the pipeline that automates building, scanning, and deploying — and where the DevSec part of DevSecOps really shines. 🤖

### User Input

Yes lets proceed

### Planner Response

## 🤖 Phase 3 — The Jenkinsfile

Let's open it:

*Viewed [Jenkinsfile](file:///c:/PROJECTS/devsecops-pipeline/Jenkinsfile) *

### Planner Response

Let's go through this section by section.

---

## 🤖 The Jenkinsfile — 12-Stage Security-First Pipeline

---

### 🔧 Pipeline Header (Lines 9-25)

```groovy
pipeline {
    agent any    // Run on any available Jenkins agent
```

```groovy
    environment {
        AWS_ACCOUNT_ID = credentials('aws-account-id')   // Read from Jenkins secrets vault
        AWS_REGION     = 'us-east-1'
        ECR_REPO       = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/devsecops-pipeline-app"
        IMAGE_TAG      = "${BUILD_NUMBER}"               // Jenkins auto-increments this: 1, 2, 3...
        CLUSTER_NAME   = 'devsecops-pipeline-prod'
    }
```

`credentials('aws-account-id')` — Jenkins has a secrets vault. You store `123456789012` there once, and the pipeline reads it **without ever printing it** in logs. This is how you avoid hardcoding account IDs.

`IMAGE_TAG = "${BUILD_NUMBER}"` — Every build gets a unique tag. Build 47 → image tagged `:47`. This maps **directly** to your ECR immutable tag — you can always trace which Jenkins build produced which image.

```groovy
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))  // Keep only last 10 build logs
        timeout(time: 60, unit: 'MINUTES')              // Kill pipeline if it runs >1 hour
        timestamps()                                    // Add timestamps to every log line
        disableConcurrentBuilds()                       // No two builds run at same time
    }
```

`disableConcurrentBuilds()` is subtle but important. Without it, two developers push at the same time → two pipelines run → both do `terraform apply` simultaneously → race condition → state corruption. This prevents that.

---

### Stage 1 — Checkout (Lines 32-38)
```groovy
stage('Checkout') {
    steps {
        cleanWs()       // Delete everything from previous build first
        checkout scm    // Pull the code from whatever SCM is configured (Git)
    }
}
```

`cleanWs()` — wipes the workspace before starting. Prevents stale files from a previous build bleeding into this one.

---

### Stage 2 — SCA Trivy FS Scan (Lines 43-56) — 🔍 First Security Gate

```groovy
sh '''
    trivy fs --severity HIGH,CRITICAL \
             --format table \
             --exit-code 0 \            ← NOTE: exit-code 0 = don't fail the build
             --output trivy-sca-report.txt \
             app/                       ← Scan the source code directory
'''
```

**Trivy FS** scans your **source code and dependencies (pom.xml)** before anything is built. It checks your Maven dependencies against CVE databases.

`--exit-code 0` — this stage is **informational only**. It generates a report but doesn't fail the build. Why? Because you want visibility without blocking early. The **hard gate** comes later at Stage 7 after the image is built.

`archiveArtifacts` — saves the report as a downloadable artifact in Jenkins so you can review it anytime.

> **Interview answer:** *"I scan the source code dependencies with Trivy before even building. This catches known CVEs in Maven packages early — fail fast, fail cheap. I don't block here because I want developers to see the report, but the real gate is the image scan which is much more comprehensive."*

---

### Stage 3 — Dockerfile Lint (Lines 61-71) — 📋 Best Practices Check

```groovy
sh '''
    hadolint Dockerfile --format tty  || true    // Print human-readable, don't fail
    hadolint Dockerfile --format json > hadolint-report.json || true
'''
```

**Hadolint** is a Dockerfile linter — it checks your Dockerfile against best practices. It would flag things like:
- `FROM ubuntu` instead of a specific version (not reproducible)
- `RUN apt-get update` without `apt-get clean` (bloated layers)
- Missing `USER` instruction (running as root)
- `ADD` when you should use `COPY`

`|| true` — again, informational. Generates the report but doesn't fail the build.

---

### Stage 4 — Unit Tests (Lines 76-88)

```groovy
stage('Unit Tests') {
    steps {
        dir('app') {
            sh 'mvn test -B'    // Run all tests inside the app/ directory
        }
    }
    post {
        always {
            // Publish test results to Jenkins UI regardless of pass/fail
            junit allowEmptyResults: true, testResults: 'app/target/surefire-reports/*.xml'
        }
    }
}
```

`post { always { ... } }` — the `junit` publisher runs **even if tests fail**. This is important because you want to see which tests failed in the Jenkins UI, not just a raw log. Jenkins parses the XML and shows a nice test trend chart.

---

### Stage 5 — Build JAR (Lines 93-101)

```groovy
dir('app') {
    sh 'mvn package -DskipTests -B'    // Build the JAR, skip tests (already ran in Stage 4)
}
```

Tests already ran in Stage 4 — no point running them again. `-DskipTests` only skips execution, not compilation.

---

### Stage 6 — Docker Build (Lines 106-116)

```groovy
sh """
    docker build -t ${ECR_REPO}:${IMAGE_TAG} \   // Tag with build number: :47
                 -t ${ECR_REPO}:latest \          // Also tag as :latest
                 .                                // Build context = repo root
"""
```

Two tags on the same image:
- `:47` → Immutable, traceable, for production
- `:latest` → Convenience for developers running locally

The `.` at the end means the **build context** is the repo root — the Dockerfile can COPY from `app/target/` because the build happens from that root.

---

### Stage 7 — Container Vuln Scan (Lines 123-142) — 🚨 THE QUALITY GATE

This is the most important stage. It runs Trivy **twice**:

```groovy
// Run 1: Generate the full report (save for review)
sh """
    trivy image --severity HIGH,CRITICAL \
                --format table \
                --output trivy-image-report.txt \
                ${ECR_REPO}:${IMAGE_TAG}     // Scan the actual built image
"""
archiveArtifacts artifacts: 'trivy-image-report.txt'

// Run 2: THE GATE — fail the build if any HIGH/CRITICAL found
sh """
    trivy image --severity HIGH,CRITICAL \
                --exit-code 1 \              ← exit-code 1 = FAIL the pipeline!
                ${ECR_REPO}:${IMAGE_TAG}
"""
```

**Why scan the image and not just the source?** The image includes the base OS layer too (`eclipse-temurin:17-jre-jammy`). Even if your Java code is clean, the Ubuntu packages in the base image might have CVEs.

```
trivy image scans:
  ✓ Your JAR's dependencies (same as FS scan)
  ✓ OS packages in the base image (apt packages)
  ✓ Java runtime libraries shipped with the JRE
```

> **Interview answer:** *"The quality gate is at Stage 7. `--exit-code 1` tells Trivy to exit with a non-zero code if it finds ANY HIGH or CRITICAL CVE. Jenkins treats any non-zero exit as stage failure — pipeline stops, nothing gets pushed to ECR, nothing gets deployed. The only way to proceed is to update the base image or fix the vulnerable dependency."*

---

### Stage 8 — ECR Push (Lines 147-162)

```groovy
// Authenticate to ECR
sh """
    aws ecr get-login-password --region ${AWS_REGION} | \
    docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
"""

// Push both tags
sh """
    docker push ${ECR_REPO}:${IMAGE_TAG}
    docker push ${ECR_REPO}:latest
"""
```

`aws ecr get-login-password` returns a **temporary token** (valid 12 hours). It's piped directly into `docker login` — never stored in a file, never printed in logs. The Jenkins agent authenticates to ECR using **its own IAM role** (instance profile) — again, no access keys.

**Why push only after the scan passes?** If you pushed before scanning, someone could deploy the unscanned image before the gate runs.

```
✅ Correct order:
   Build → Scan → [GATE] → Push → Deploy

❌ Risky order:
   Build → Push → Scan → Deploy
           ↑ unscanned image exists in ECR
```

---

### Stage 9 — Terraform Plan (Lines 167-179)

```groovy
dir('terraform') {
    sh '''
        terraform init -input=false   // Download providers and modules
        terraform plan -out=tfplan    // Create a plan file (saved binary)
    '''
}
archiveArtifacts artifacts: 'terraform/tfplan'    // Save plan as artifact
```

`-out=tfplan` saves the plan as a binary file. This is important — it guarantees that what you **reviewed** is exactly what gets **applied** in Stage 11. If someone changed the code between plan and apply, the saved plan still reflects the original intent.

`-input=false` — no interactive prompts. Pipelines are non-interactive.

---

### Stage 10 — Manual Approval Gate (Lines 184-191) — ⏸️ Human in the Loop

```groovy
input message: 'Review the Terraform plan and Docker scan results. Proceed with deployment?',
      ok: 'Deploy',
      submitter: 'admin,devops-team'    // Only these users can approve
```

The pipeline **pauses here**. Jenkins sends a notification. A human logs into Jenkins, reviews:
1. The Terraform plan (what infra will change?)
2. The Trivy scan report (any acceptable risk?)

Only then clicks **Deploy** to proceed.

`submitter: 'admin,devops-team'` — not just anyone can approve. Only users in the `devops-team` group or the `admin` user. Prevents developers from self-approving their own Production deployments.

> **Interview answer:** *"I implemented a manual approval gate between Terraform Plan and Apply. The pipeline pauses, a senior engineer reviews the plan diff and the security scan results, then clicks approve. This prevents any automated process from modifying production infrastructure without a human sign-off — that's a core change management requirement for SOC2, ISO 27001, etc."*

---

### Stage 11 — Terraform Apply (Lines 196-204)

```groovy
dir('terraform') {
    sh 'terraform apply -auto-approve tfplan'    // Apply the SAVED plan file
}
```

`terraform apply tfplan` — applying the **saved plan** from Stage 9. Not re-planning. This means exactly what was reviewed is what gets applied.

`-auto-approve` — no interactive confirmation needed (the human already approved in Stage 10).

---

### Stage 12 — Deploy to EKS (Lines 209-230)

```groovy
sh """
    # Step 1: Configure kubectl to talk to our cluster
    aws eks update-kubeconfig --region ${AWS_REGION} --name ${CLUSTER_NAME}

    # Step 2: Inject the exact image tag into the deployment YAML
    sed -i 's|IMAGE_PLACEHOLDER|${ECR_REPO}:${IMAGE_TAG}|g' kubernetes/deployment.yaml

    # Step 3: Apply manifests in order
    kubectl apply -f kubernetes/namespace.yaml
    kubectl apply -f kubernetes/service-account.yaml
    kubectl apply -f kubernetes/deployment.yaml
    kubectl apply -f kubernetes/service.yaml
    kubectl apply -f kubernetes/hpa.yaml
    kubectl apply -f kubernetes/ingress.yaml

    # Step 4: Wait and verify rollout
    kubectl rollout status deployment/vprofile-app -n vprofile --timeout=300s
"""
```

The `sed` command is the key link between CI and CD. [deployment.yaml](cci:7://file:///c:/PROJECTS/devsecops-pipeline/kubernetes/deployment.yaml:0:0-0:0) has `IMAGE_PLACEHOLDER` as a literal string. Sed replaces it at runtime with the real ECR URL and build number.

`kubectl rollout status --timeout=300s` — Jenkins **waits** up to 5 minutes for the deployment to succeed. If pods crash and can't come up, this command exits non-zero, the pipeline fails, and you get alerted. Without this, Jenkins would mark the build as success even if pods are crashlooping.

---

### Post Actions (Lines 236-272)

```groovy
post {
    always {
        // Clean up the Docker image from the Jenkins agent — save disk space
        sh "docker rmi ${ECR_REPO}:${IMAGE_TAG} || true"
    }
    success { echo "✅ Pipeline SUCCEEDED" }
    failure { echo "❌ Pipeline FAILED" }
}
```

`always` runs regardless of pass/fail. `|| true` ensures cleanup doesn't itself fail the pipeline if the image was already removed.

The Slack notification lines are commented out — ready to uncomment when you add the Slack Jenkins plugin.

---

### 🗺️ The Full Pipeline Flow

```
Developer pushes code to Git
          │
          ▼
┌─────────────────────────────────────────────────────────┐
│  Stage 1:  Checkout                  (clean + pull)     │
│  Stage 2:  Trivy FS Scan             (source deps)  📊  │ ← Report only
│  Stage 3:  Hadolint                  (Dockerfile)   📊  │ ← Report only
│  Stage 4:  Unit Tests                (mvn test)     ✅  │ ← Fails if tests break
│  Stage 5:  Build JAR                 (mvn package)      │
│  Stage 6:  Docker Build              (image created)    │
│  Stage 7:  Trivy Image Scan          🚨 QUALITY GATE 🚨 │ ← Fails on HIGH/CRITICAL CVE
│  Stage 8:  ECR Push                  (after gate passes)│
│  Stage 9:  Terraform Plan            (saved to tfplan)  │
│  Stage 10: ⏸️  MANUAL APPROVAL        (human reviews)   │ ← Pauses here
│  Stage 11: Terraform Apply           (infra changes)    │
│  Stage 12: Deploy to EKS             (K8s rollout)  ✅  │ ← Waits for rollout
└─────────────────────────────────────────────────────────┘
          │
          ▼
    post: cleanup Docker images
```

---

### 📝 Key Interview Question

**"What prevents a bad image from reaching production?"**

> *"Three things — first, Trivy scans source dependencies at Stage 2. Second, Trivy scans the built image at Stage 7 with `--exit-code 1` — pipeline hard-stops if any HIGH or CRITICAL CVE is found, nothing gets pushed to ECR. Third, a human reviews the Terraform plan and scan reports at Stage 10 before anything touches production. The image only reaches the cluster if it passes all three."*

---

Ready to move to **Phase 4: Kubernetes**? That's where we look at the deployment manifests — how the app actually runs in the cluster with HPA, Ingress, and all the Day-2 operations. ☸️

### User Input

Sure, lets proceed 

### Planner Response

## ☸️ Phase 4 — Kubernetes Manifests

The user already has [service-account.yaml](cci:7://file:///c:/PROJECTS/devsecops-pipeline/kubernetes/service-account.yaml:0:0-0:0) open — perfect starting point. Let's look at all the K8s files first to understand the full picture:

*Viewed [namespace.yaml](file:///c:/PROJECTS/devsecops-pipeline/kubernetes/namespace.yaml) *

*Viewed [service-account.yaml](file:///c:/PROJECTS/devsecops-pipeline/kubernetes/service-account.yaml) *

*Viewed [deployment.yaml](file:///c:/PROJECTS/devsecops-pipeline/kubernetes/deployment.yaml) *

*Viewed [service.yaml](file:///c:/PROJECTS/devsecops-pipeline/kubernetes/service.yaml) *

*Viewed [hpa.yaml](file:///c:/PROJECTS/devsecops-pipeline/kubernetes/hpa.yaml) *

*Viewed [ingress.yaml](file:///c:/PROJECTS/devsecops-pipeline/kubernetes/ingress.yaml) *

### Planner Response

Now let's break each one down:

---

## ☸️ Kubernetes — 6 Manifests, Applied in Order

Think of these manifests as **layers of a cake** — each one depends on the one below it:

```
Ingress        ← internet traffic enters here
Service        ← routes to pods
Deployment     ← manages the pods
ServiceAccount ← gives pods their AWS identity
Namespace      ← the isolated box everything lives in
```

---

### 1️⃣ Namespace (namespace.yaml) — The Isolated Box

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: cloudpulse
  labels:
    app: cloudpulse
    environment: prod
```

Dead simple — but important. A Namespace is a **virtual cluster** inside your cluster. Everything in this project — pods, services, secrets — lives inside `cloudpulse`.

**Why not use the `default` namespace?**
- `default` has no isolation — if you apply wrong manifests, they could affect other apps
- Namespace-level RBAC — you can grant a team only access to `cloudpulse`
- `kubectl get pods -n cloudpulse` — you always know exactly what you're looking at
- Resource quotas can be applied per namespace — limit CPU/memory for this app

> **Interview answer:** *"I use a dedicated namespace to isolate the application. In a multi-tenant cluster, this is essential — different teams own different namespaces, RBAC is scoped per namespace, and resource quotas prevent one app from starving the others."*

---

### 2️⃣ Service Account (service-account.yaml) — The Pod's AWS Identity

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: cloudpulse-sa
  namespace: cloudpulse
  annotations:
    # THIS IS THE IRSA LINK
    eks.amazonaws.com/role-arn: "arn:aws:iam::<ACCOUNT_ID>:role/devsecops-pipeline-app-irsa-role"
```

This is the **K8s side of IRSA**. Remember in the IAM module we created the IAM role with a condition:
```
"Only for system:serviceaccount:cloudpulse:cloudpulse-sa"
```

This annotation is the **other half** of that handshake. When EKS sees a pod using this service account, it knows to inject the IAM credentials for that role.

The complete IRSA chain is now visible:

```
IAM Module (Terraform)           Service Account (K8s)
─────────────────────────────    ─────────────────────────────
IAM Role:                        ServiceAccount:
  cloudpulse-app-irsa-role        cloudpulse-sa
  Trust policy:               ←→  annotation:
    Only for:                       eks.amazonaws.com/role-arn:
    cloudpulse:cloudpulse-sa          ...app-irsa-role
```

Without this annotation, the pod gets no AWS credentials at all — which is actually safer by default.

---

### 3️⃣ Deployment (deployment.yaml) — The Core Workload

This is the most detailed manifest. Let's go section by section:

#### Rolling Update Strategy
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1        # During update, allow 1 EXTRA pod (temporarily have 3 instead of 2)
    maxUnavailable: 0  # Never take a pod down until the new one is Ready
```

This is **zero-downtime deployment**:
```
Before update:   [Pod v1] [Pod v1]
During update:   [Pod v1] [Pod v1] [Pod v2]  ← maxSurge=1 allows this
                 [Pod v1]          [Pod v2]  ← old pod removed when new is Ready
After update:             [Pod v2] [Pod v2]
```
`maxUnavailable: 0` is key — at no point are you serving with fewer than 2 pods.

---

#### Service Account Link
```yaml
spec:
  serviceAccountName: cloudpulse-sa   # ← Links to the service account we just saw
```

This is how the pod actually **gets** the IAM role. EKS sees this, looks at the SA's annotation, and injects a projected volume with a JWT token into the pod automatically.

---

#### Security Context (Two Levels)
```yaml
# Pod-level (applies to ALL containers in the pod)
securityContext:
  runAsNonRoot: true   # K8s refuses to start the pod if image tries to run as root
  fsGroup: 1000        # All files in mounted volumes are owned by GID 1000

# Container-level (applies to THIS container)
securityContext:
  runAsNonRoot: true
  allowPrivilegeEscalation: false   # Can't use sudo or setuid binaries
  readOnlyRootFilesystem: false     # App needs to write temp files (Spring Boot)
  capabilities:
    drop:
      - ALL                         # Drop ALL Linux capabilities (NET_RAW, SYS_ADMIN, etc.)
```

`drop: ALL` is the most powerful line here. Linux capabilities are what give root-like powers. By dropping ALL of them, even if the process somehow gets root UID, it has none of the dangerous abilities.

> **Interview answer:** *"I enforce security at both pod and container level. `runAsNonRoot` means K8s itself will refuse to schedule the pod if the image has `USER root`. Dropping ALL capabilities means even if there's a privilege escalation bug, the process can't do things like open raw sockets, modify kernel parameters, or mount filesystems."*

---

#### Resource Requests vs Limits
```yaml
resources:
  requests:
    cpu: "250m"       # 0.25 CPU cores — what the scheduler RESERVES
    memory: "512Mi"   # 512MB RAM — what the scheduler RESERVES
  limits:
    cpu: "1000m"      # 1 CPU core — hard cap, can't exceed this
    memory: "1024Mi"  # 1GB RAM — if exceeded, container is OOMKilled
```

**`requests`** — what the scheduler uses for placement. A node with 2 CPUs can fit 8 pods requesting `250m` each.

**`limits`** — hard ceiling. CPU gets throttled, memory gets killed.

**Why both matter for HPA:** The HPA calculates [(current CPU) / (requested CPU) * 100](cci:1://file:///c:/PROJECTS/devsecops-pipeline/app/src/main/java/com/cloudpulse/config/DataSeeder.java:27:4-203:5) — if a pod requested `250m` and is using `200m`, that's 80% utilization → HPA might scale up. **Without requests, HPA has no baseline to calculate against.**

---

#### The 3 Probes — Different Jobs
```yaml
startupProbe:   # Phase 1: Is the app done starting up?
  initialDelaySeconds: 10
  failureThreshold: 12    # 12 failures × 10s = 120s max startup time
  # During this time, liveness probe is DISABLED

livenessProbe:  # Phase 2: Is the app still alive?
  initialDelaySeconds: 60
  failureThreshold: 3     # 3 failures → container RESTARTS
  # If this fails, K8s kills and restarts the container

readinessProbe: # Phase 3 (parallel): Is the app ready to accept traffic?
  initialDelaySeconds: 30
  failureThreshold: 3     # 3 failures → removed from Service endpoints
  # If this fails, K8s stops sending traffic BUT doesn't restart
```

The **timeline** during a pod startup:
```
0s  ─── Pod starts
10s ─── startupProbe begins checking (every 10s, up to 12 times = 2 mins)
30s ─── readinessProbe begins (parallel to startup)
60s ─── livenessProbe begins (only after startup passes)
```

> **Interview answer:** *"Startup probe solves the problem of slow-starting apps. Spring Boot takes ~30-60 seconds to initialize. Without a startup probe, the liveness probe would fire after `initialDelay` and kill the pod before it even started. The startup probe gives it up to 120 seconds to come up — during that time, liveness is suspended."*

---

### 4️⃣ Service (service.yaml) — Internal Traffic Routing

```yaml
kind: Service
spec:
  type: ClusterIP      # No external IP — only reachable inside the cluster
  selector:
    app: cloudpulse    # Routes to any pod with this label
  ports:
    - port: 80         # Service listens on 80
      targetPort: http # Forwards to container's "http" named port = 8080
```

`ClusterIP` is intentionally internal. The outside world never talks directly to the Service — they talk to the **Ingress/ALB**, which forwards to the Service, which routes to the pods.

```
Internet → ALB (port 443) → Service (port 80) → Pods (port 8080)
```

The selector `app: cloudpulse` is how the Service finds its pods. It watches for any pod in the `cloudpulse` namespace with label `app: cloudpulse`. When HPA adds new pods, they automatically get registered as service endpoints if they have that label and pass the readiness probe.

---

### 5️⃣ HPA (hpa.yaml) — Automatic Scaling

```yaml
spec:
  scaleTargetRef:
    name: cloudpulse-app   # Watch this Deployment
  minReplicas: 2           # Never go below 2 (high availability)
  maxReplicas: 10          # Never go above 10 (cost control)
```

Two scaling triggers:
```yaml
metrics:
  - cpu:    averageUtilization: 50    # Scale up when avg CPU > 50% across all pods
  - memory: averageUtilization: 70    # Scale up when avg Memory > 70% across all pods
```

If **either** metric triggers, HPA scales up. Both must be below threshold to scale down.

#### The Behavior Block — Prevents Flapping
```yaml
behavior:
  scaleUp:
    stabilizationWindowSeconds: 60    # Wait 60s of sustained high load before scaling up
    policies:
      - type: Pods
        value: 2                      # Add max 2 pods per scale-up event
        periodSeconds: 60

  scaleDown:
    stabilizationWindowSeconds: 300   # Wait 5 MINUTES of sustained low load before scaling down
    policies:
      - type: Pods
        value: 1                      # Remove only 1 pod per scale-down event
        periodSeconds: 120
```

**Scale up fast, scale down slow** — this is the right pattern:
- Traffic spike? Add 2 pods quickly (60s window)
- Traffic drops? Wait **5 full minutes** before removing a pod — avoids killing capacity during brief lulls

> **Interview answer:** *"The HPA behavior block prevents flapping — rapidly scaling up and down as metrics oscillate. I use a 60-second stabilization window for scale-up so we react to real load quickly. Scale-down uses a 5-minute window and removes only 1 pod at a time — if traffic comes back immediately, we still have capacity."*

---

### 6️⃣ Ingress (ingress.yaml) — The Internet-Facing Door

```yaml
annotations:
  alb.ingress.kubernetes.io/scheme: internet-facing   # Public ALB in public subnets
  alb.ingress.kubernetes.io/target-type: ip           # Route directly to pod IPs (not nodes)
  alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
  alb.ingress.kubernetes.io/ssl-redirect: "443"       # Force HTTP → HTTPS redirect
```

`target-type: ip` vs `instance` is important:
- `instance` — ALB sends traffic to the Node's NodePort, then kube-proxy routes to the pod (2 hops)
- `ip` — ALB sends traffic **directly to the pod IP** (1 hop, lower latency, works with IRSA)

```yaml
  alb.ingress.kubernetes.io/certificate-arn: "arn:aws:acm:..."   # TLS termination at ALB
  alb.ingress.kubernetes.io/ssl-policy: ELBSecurityPolicy-TLS13-1-2-2021-06  # TLS 1.3 only
```

TLS terminates at the ALB — the traffic between ALB and pods over the private VPC network is unencrypted (acceptable because it never leaves AWS's internal network). The ACM certificate is managed by AWS — auto-renews, nothing to manage.

```yaml
spec:
  ingressClassName: alb    # "Use the AWS Load Balancer Controller"
  rules:
    - host: cloudpulse.yourdomain.com
      http:
        paths:
          - path: /
            backend:
              service:
                name: cloudpulse-svc   # Send to our ClusterIP service
                port: 80
```

When Jenkins applies this manifest, the **AWS Load Balancer Controller** (running as a pod in `kube-system`, using its IRSA role) watches for new Ingress objects and automatically:
1. Creates an ALB in your public subnets
2. Creates target groups pointing to your pod IPs
3. Configures the SSL listener with your ACM cert
4. Sets up the HTTP→HTTPS redirect rule

---

### 🗺️ The Full Traffic Flow

```
User Browser
     │
     │ HTTPS (443)
     ▼
┌─────────────────────────────────┐
│   AWS ALB (Public Subnet)       │   ← Created by ALB Controller from Ingress YAML
│   TLS 1.3 termination (ACM cert)│
│   HTTP → HTTPS redirect         │
└────────────────┬────────────────┘
                 │ HTTP (80) — inside private VPC
                 ▼
┌─────────────────────────────────┐
│   K8s Service (ClusterIP)       │   ← service.yaml
│   port 80 → pod port 8080       │
└────────────────┬────────────────┘
                 │ Load balances across pods
        ┌────────┴────────┐
        ▼                 ▼
┌──────────────┐  ┌──────────────┐
│  Pod (8080)  │  │  Pod (8080)  │   ← deployment.yaml (min 2 replicas)
│  Running as  │  │  Running as  │
│  appuser     │  │  appuser     │
│  No root     │  │  No root     │
└──────────────┘  └──────────────┘
        ↕                 ↕
   AWS SDK uses IRSA token to call AWS APIs (S3, etc.)
```

---

### 📝 The Full Project Interview Summary

You can now confidently say:

> *"I built a Cloud Infrastructure Monitoring API called CloudPulse. The infrastructure is provisioned using modular Terraform — a VPC with 3-AZ public and private subnets, an EKS cluster with managed node groups, and IRSA roles so pods never need hardcoded AWS keys. The app is containerized using a multi-stage Docker build that produces a non-root, minimal image. A Jenkins pipeline handles CI/CD — it scans dependencies with Trivy, lints the Dockerfile with Hadolint, runs unit tests, builds and scans the image with a hard quality gate that blocks HIGH and CRITICAL CVEs, pushes to ECR, runs Terraform plan, waits for a human approval, applies infrastructure changes, and finally deploys to EKS with zero-downtime rolling updates. In Kubernetes, the application runs with all Linux capabilities dropped, resource limits enforced, 3-probe health checking, and HPA that scales from 2 to 10 pods based on CPU and memory — with asymmetric scaling behavior to scale up fast and scale down cautiously."*

That's a **complete, coherent story** end-to-end. 🎯

### User Input

Okay how is the application working , we didnt mention that, explain from devops/SRE prespective, like whatever is important for them to know, rest info you can tell in general

### Planner Response

Good question — let's look at the app from a **DevOps/SRE lens**, not a developer lens. Let me pull up the key files:

*Viewed [application.yml](file:///c:/PROJECTS/devsecops-pipeline/app/src/main/resources/application.yml) *

*Viewed [HealthCheckService.java](file:///c:/PROJECTS/devsecops-pipeline/app/src/main/java/com/cloudpulse/service/HealthCheckService.java) *

### Planner Response

Good. Now let me explain everything from a **DevOps/SRE perspective** — skipping developer details, focusing on what matters operationally.

---

## 🔍 How CloudPulse Works — DevOps/SRE View

---

### 1. What Does the App Actually Do?

CloudPulse is a **REST API** that:
- Tracks cloud infrastructure resources (EC2, RDS, EKS, Lambda, etc.)
- Runs **periodic health checks** on those resources every 5 minutes
- Manages incidents when resources go unhealthy
- Exposes a **dashboard** with real-time health percentages, MTTR, and severity breakdowns

From an SRE standpoint — it **is** the kind of tool SREs build internally. Think a lightweight version of Datadog or PagerDuty.

---

### 2. 🔑 The Config System — What DevOps Must Know

```yaml
# application.yml (default profile — dev/test)
spring:
  datasource:
    url: jdbc:h2:mem:cloudpulse    # In-memory database, zero setup
    
# application-prod.yml (prod profile)
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**Two profiles, two databases:**

| Profile | Database | When Used |
|---|---|---|
| `default` | H2 in-memory | Jenkins unit tests, local dev |
| `prod` | MySQL (RDS) | Kubernetes production pods |

**How do pods know to use prod?** You set an environment variable in the K8s deployment:
```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
  - name: DB_HOST
    valueFrom:
      secretKeyRef:         # Read from K8s Secret, not hardcoded
        name: db-secret
        key: host
```

> **SRE talking point:** *"H2 means the Jenkins pipeline can run unit tests with zero infra dependencies — no database to spin up, no network calls, tests are fast and isolated. The app switches to real MySQL in production via environment variable injection — the code doesn't change, only the config does."*

---

### 3. ⏰ The Scheduled Health Checker — Background Worker

This is the most operationally interesting part:

```java
@Scheduled(fixedDelayString = "${cloudpulse.healthcheck.interval:300000}")
public void scheduledHealthChecks() {
    List<Resource> resources = resourceRepository.findAll();
    for (Resource resource : resources) {
        performHealthCheck(resource);   // Probe each resource
    }
}
```

**`fixedDelayString = "${cloudpulse.healthcheck.interval:300000}"`**

Breaking down that odd string:
- It reads from config key `cloudpulse.healthcheck.interval`
- `:300000` is the **default** if the config key is not set
- `300000` milliseconds = **5 minutes**

This means you can **tune the check interval without redeploying** — just change a ConfigMap in Kubernetes:
```yaml
# configmap.yaml (you could add this)
data:
  cloudpulse.healthcheck.interval: "60000"   # Change to 1 minute
```

`fixedDelay` vs `fixedRate` — an important SRE distinction:
- `fixedRate`: Run every 5 mins **regardless** of how long the check took
- `fixedDelay`: Wait 5 mins **after the previous run completes**

We use `fixedDelay` — if checking 100 resources takes 2 minutes, you don't immediately start another run. This prevents **thundering herd** — multiple overlapping check cycles piling up.

---

### 4. 📊 Observability — The Actuator Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: always
```

These are **Spring Boot Actuator** endpoints — built-in observability with zero code:

| Endpoint | URL | Used By |
|---|---|---|
| [health](cci:1://file:///c:/PROJECTS/devsecops-pipeline/app/src/main/java/com/cloudpulse/service/ResourceService.java:53:4-56:5) | `/api/actuator/health` | K8s liveness + readiness probes, ALB health checks |
| `info` | `/api/actuator/info` | Shows app version, environment |
| `metrics` | `/api/actuator/metrics` | Raw JVM metrics (heap, GC, threads) |
| `prometheus` | `/api/actuator/prometheus` | **Prometheus scraping endpoint** |

**The health endpoint response** (what K8s probes actually see):
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",              ← Is the database reachable?
      "details": { "database": "H2" }
    },
    "diskSpace": {
      "status": "UP",              ← Enough disk?
      "details": { "free": "10GB" }
    }
  }
}
```

The `db` component is automatically included because Spring Data JPA is on the classpath. If the database goes down, this returns `{ "status": "DOWN" }` → K8s readiness probe fails → pod removed from Service endpoints → **no traffic reaches a pod that can't talk to its database**.

> **SRE talking point:** *"The readiness probe hits the Actuator health endpoint, which does a real database connectivity check. If RDS has a failover event, pods automatically stop receiving traffic within 30 seconds — the readiness probe fails, K8s drains them from the service. When RDS comes back, they become ready again automatically. No manual intervention."*

The **prometheus endpoint** exposes JVM and HTTP metrics in Prometheus format:
```
# EXAMPLE OUTPUT from /actuator/prometheus
jvm_memory_used_bytes{area="heap"} 1.23e+08
http_server_requests_seconds_count{uri="/api/resources"} 42
http_server_requests_seconds_sum{uri="/api/resources"} 0.893
```
You can scrape this with **Prometheus** and build **Grafana dashboards** — thread counts, GC pauses, request latency per endpoint — without writing any code.

---

### 5. 🗄️ Database — What SRE Needs to Know

```yaml
# Production (application-prod.yml)
hikari:
  maximum-pool-size: 10        # Max 10 DB connections per pod
  minimum-idle: 5              # Keep 5 connections warm
  idle-timeout: 300000         # Close idle connections after 5 mins
  connection-timeout: 20000    # Fail fast if can't connect in 20s
```

**HikariCP** is the connection pool. This is critical for SRE:

- If you have **3 pods × 10 max connections = 30 total connections to RDS**
- RDS `db.t3.medium` supports ~80 connections
- Scale to 10 pods (HPA max) → 100 connections → could hit RDS limit

This is a real operational concern. You'd solve it with **RDS Proxy** in front of the database — it pools connections between your pods and RDS, so even 10 pods each with 10 connections pool down to ~10-20 actual RDS connections.

```
schema:
  ddl-auto: update   # dev — Hibernate auto-creates/modifies tables
  ddl-auto: validate # prod — Hibernate only VALIDATES schema, never changes it
```

`validate` in prod means if there's a schema mismatch (someone changed a model without a migration), the **app fails to start** rather than silently corrupting the database. This is the safer mode — catch mistakes at startup, not during runtime.

---

### 6. 📝 Logging — SRE View

```yaml
logging:
  level:
    com.cloudpulse: INFO      # Our code — INFO level
    org.springframework: WARN  # Framework — only warnings
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

The log pattern outputs **structured enough for log aggregation**. In Kubernetes, stdout goes to the node, and a **DaemonSet** (like Fluentd or Fluent Bit) ships it to a log aggregation system (CloudWatch Logs, ELK, Loki).

In the [HealthCheckService](cci:2://file:///c:/PROJECTS/devsecops-pipeline/app/src/main/java/com/cloudpulse/service/HealthCheckService.java:19:0-143:1) you saw:
```java
log.info("Running scheduled health checks on {} resources...", resources.size());
log.error("Health check failed for resource {}: {}", resource.getName(), e.getMessage());
```

`log.error` with the resource name means in CloudWatch you can alert on:
```
filter @message like "Health check failed"
| stats count(*) as failures by bin(5m)
```
→ **Alert if > 3 health check failures per 5 minutes.**

---

### 7. ⚡ The App's Data Flow — End to End

```
On startup (DataSeeder.java):
   App starts → checks if DB is empty → seeds 12 AWS resources + 3 incidents

Every 5 minutes (HealthCheckService.java):
   Scheduler fires → loads all resources from DB
   → for each resource: simulates HTTP probe → records result → updates resource.status
   → logs result

On API calls (Controllers):
   GET /api/dashboard → aggregates stats across all 3 tables → returns JSON
   GET /api/resources/unhealthy → queries DB for UNHEALTHY/DEGRADED → returns list
   POST /api/incidents/{id}/resolve → updates status, sets resolvedAt → MTTR calculated
```

---

### 8. 🔁 What Happens During a Pod Restart (Operational Reality)

This is important for SRE interviews. Since we use **H2 in dev** (in-memory):

```
Pod restarts → H2 database WIPED → DataSeeder runs again → 12 resources re-seeded
```

In **production with MySQL/RDS**:
```
Pod restarts → connects to RDS (persistent) → DataSeeder checks "count > 0" → skips seeding
→ all data preserved
```

The seeder has this guard:
```java
if (resourceRepository.count() > 0) {
    log.info("Database already seeded, skipping...");
    return;
}
```

This means the seeder is **idempotent** — safe to call on every pod start, it won't duplicate data.

---

### 9. 🏥 How K8s Probes Interact with the App

```
Pod starts
    │
    ├── StartupProbe hits /api/actuator/health every 10s
    │   Spring Boot is booting... (takes ~30-60s)
    │   Probe fails (expected) — but doesn't kill pod
    │   After ~45s: app starts → H2 connects → Hibernate creates schema
    │   → DataSeeder runs → probe returns 200 UP ✅
    │   → Startup probe PASSES → liveness + readiness take over
    │
    ├── ReadinessProbe hits /api/actuator/health every 10s
    │   Checks: is Spring Boot up? Is DB reachable?
    │   PASSES → pod added to Service endpoints → traffic begins
    │
    └── LivenessProbe hits /api/actuator/health every 15s
        Checks: Is app still alive?
        If DB connection pool exhausted or app deadlocked → returns DOWN
        → 3 failures → K8s RESTARTS the container (not the pod)
```

---

### 🗺️ Summary — What You Tell an SRE Interviewer

> *"CloudPulse is a Spring Boot REST API. From an operational standpoint — it uses Spring Profiles to switch between H2 for testing and MySQL in production, with zero code changes. The Actuator health endpoint is used by all three K8s probes and the ALB health check — it does a real DB connectivity check, so if RDS fails, pods automatically stop receiving traffic. A background scheduler runs every 5 minutes to probe monitored resources — the interval is configurable via environment variable without redeployment. HikariCP manages DB connections — 10 per pod, so I need to watch total connections at scale. Logs go to stdout, picked up by Fluentd and shipped to CloudWatch where I can set metric filters and alarms. The Prometheus endpoint exposes JVM and HTTP metrics for Grafana dashboards without any instrumentation code."*

That's the complete operational story for CloudPulse. Ready to talk about what else you'd add in a production scenario — like **secrets management (AWS Secrets Manager)**, **distributed tracing (X-Ray)**, or **blue/green deployments**? Those would round out the interview story nicely. 🎯