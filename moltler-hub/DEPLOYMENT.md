# MoltlerHub Deployment Guide

This guide covers deploying MoltlerHub to Google Cloud Run with the custom domain `hub.moltler.dev`.

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  GitHub Push    │────►│  GitHub Actions  │────►│  Cloud Run      │
│  (main branch)  │     │  CI/CD Pipeline  │     │  (Container)    │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                          │
                                                          ▼
                                               ┌─────────────────────┐
                                               │  hub.moltler.dev    │
                                               │  (Custom Domain)    │
                                               └─────────────────────┘
```

## Prerequisites

1. **GCP Project** with billing enabled
2. **GitHub repository** with the code
3. **Domain access** to configure DNS for `moltler.dev`

## One-Time GCP Setup

### Step 1: Create GCP Project (if needed)

```bash
# Set your project ID
export PROJECT_ID="your-gcp-project-id"

# Create project (optional)
gcloud projects create $PROJECT_ID --name="Moltler"

# Set as default
gcloud config set project $PROJECT_ID

# Enable billing (required for Cloud Run)
# Do this in GCP Console: https://console.cloud.google.com/billing
```

### Step 2: Enable Required APIs

```bash
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  iam.googleapis.com
```

### Step 3: Create Artifact Registry Repository

```bash
gcloud artifacts repositories create moltler-hub \
  --repository-format=docker \
  --location=us-central1 \
  --description="MoltlerHub Docker images"
```

### Step 4: Create Service Account for GitHub Actions

```bash
# Create service account
gcloud iam service-accounts create github-actions \
  --display-name="GitHub Actions Deployer"

# Get service account email
export SA_EMAIL="github-actions@${PROJECT_ID}.iam.gserviceaccount.com"

# Grant necessary permissions
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/artifactregistry.writer"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/iam.serviceAccountUser"

# Create and download key
gcloud iam service-accounts keys create gcp-sa-key.json \
  --iam-account=$SA_EMAIL

echo "Key created: gcp-sa-key.json"
echo "Add this to GitHub Secrets as GCP_SA_KEY"
```

### Step 5: Configure GitHub Secrets

Go to your GitHub repository → Settings → Secrets and variables → Actions

Add these secrets:

| Secret Name | Value |
|-------------|-------|
| `GCP_PROJECT_ID` | Your GCP project ID (e.g., `moltler-prod`) |
| `GCP_SA_KEY` | Contents of `gcp-sa-key.json` (the entire JSON) |

### Step 6: Initial Deployment (Manual)

For the first deployment, run the workflow manually:

```bash
# Or trigger via GitHub Actions UI: Actions → Deploy MoltlerHub → Run workflow
```

Alternatively, deploy manually:

```bash
cd moltler-hub

# Build locally
docker build -t us-central1-docker.pkg.dev/$PROJECT_ID/moltler-hub/moltler-hub:latest .

# Push to Artifact Registry
gcloud auth configure-docker us-central1-docker.pkg.dev
docker push us-central1-docker.pkg.dev/$PROJECT_ID/moltler-hub/moltler-hub:latest

# Deploy to Cloud Run
gcloud run deploy moltler-hub \
  --image us-central1-docker.pkg.dev/$PROJECT_ID/moltler-hub/moltler-hub:latest \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --port 8080
```

## Custom Domain Setup (hub.moltler.dev)

### Step 1: Map Custom Domain in Cloud Run

```bash
# Verify domain ownership (if not already verified)
gcloud domains verify moltler.dev

# Map the subdomain to Cloud Run
gcloud beta run domain-mappings create \
  --service moltler-hub \
  --domain hub.moltler.dev \
  --region us-central1
```

### Step 2: Get DNS Records

After creating the domain mapping, get the required DNS records:

```bash
gcloud beta run domain-mappings describe \
  --domain hub.moltler.dev \
  --region us-central1
```

This will show you the required DNS records (typically a CNAME or A records).

### Step 3: Configure DNS

Add the DNS records to your domain registrar (or Cloud DNS):

**Option A: CNAME Record (Recommended)**
```
Type: CNAME
Name: hub
Value: ghs.googlehosted.com.
TTL: 3600
```

**Option B: A Records (if CNAME not supported)**
```
Type: A
Name: hub
Value: [IP addresses from domain-mappings describe]
TTL: 3600
```

### Step 4: Verify SSL Certificate

Cloud Run automatically provisions an SSL certificate. Check status:

```bash
gcloud beta run domain-mappings describe \
  --domain hub.moltler.dev \
  --region us-central1 \
  --format='value(status.certificateStatus)'
```

Wait for status to show `ACTIVE` (can take 15-30 minutes).

## CI/CD Workflow

The GitHub Actions workflow (`.github/workflows/deploy-hub.yml`) automatically deploys when:

1. **Push to main branch** with changes in:
   - `moltler-hub/**` (hub source code)
   - `hub/skills/**` (skill definitions)
   - `.github/workflows/deploy-hub.yml` (workflow itself)

2. **Manual trigger** via GitHub Actions UI

### Workflow Steps

1. Checkout code
2. Regenerate skills data (`skills.json`)
3. Authenticate to GCP
4. Build Docker image
5. Push to Artifact Registry
6. Deploy to Cloud Run

## Monitoring & Troubleshooting

### View Logs

```bash
# Stream logs
gcloud run logs read moltler-hub --region us-central1 --tail 100

# Or in GCP Console
open "https://console.cloud.google.com/run/detail/us-central1/moltler-hub/logs"
```

### Check Service Status

```bash
gcloud run services describe moltler-hub --region us-central1
```

### View Metrics

```bash
open "https://console.cloud.google.com/run/detail/us-central1/moltler-hub/metrics"
```

### Common Issues

**1. "Permission denied" during deploy**
- Ensure service account has `roles/run.admin` and `roles/artifactregistry.writer`
- Check GCP_SA_KEY secret is correct

**2. "Domain mapping failed"**
- Verify domain ownership: `gcloud domains list`
- Check DNS propagation: `dig hub.moltler.dev`

**3. "Container failed to start"**
- Check logs for startup errors
- Verify PORT env var is set to 8080

**4. SSL certificate pending**
- DNS must be properly configured first
- Wait up to 24 hours for propagation

## Cost Estimate

Cloud Run pricing (us-central1):
- **CPU**: $0.000024/vCPU-second
- **Memory**: $0.0000025/GiB-second
- **Requests**: $0.40/million requests

With scale-to-zero and moderate traffic (~10k visits/month):
- **Estimated cost: $1-5/month**

## Local Testing

Test the Docker build locally:

```bash
cd moltler-hub

# Build
docker build -t moltler-hub:local .

# Run
docker run -p 3000:8080 moltler-hub:local

# Visit http://localhost:3000
```

## Quick Commands Reference

```bash
# Deploy manually
gcloud run deploy moltler-hub --source . --region us-central1

# View service URL
gcloud run services describe moltler-hub --region us-central1 --format 'value(status.url)'

# Update environment variables
gcloud run services update moltler-hub --region us-central1 --set-env-vars "KEY=value"

# Scale settings
gcloud run services update moltler-hub --region us-central1 --min-instances 1 --max-instances 20

# Rollback to previous revision
gcloud run services update-traffic moltler-hub --region us-central1 --to-revisions PREVIOUS_REVISION=100
```
