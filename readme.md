# Hospital Service

Implements the brief with gRPC endpoints to:

- Create/modify/delete hospitals
- Create/modify/delete patients
- Register a patient in a hospital
- List patients of a hospital
- List hospitals of a patient

Duplicate names are allowed; identity is by UUID.

### Run

- Build & start: `./gradlew bootRun` (gRPC on port 9090)

### Tests

- Run: `./gradlew test`
- Uses a **test profile** (H2 create-drop, in-process gRPC server; no TCP port).
- Covered cases:
  - Create patient
  - Create hospital
  - Two patients with same name (OK)
  - Two hospitals with same name (OK)
  - Register patient → appears in hospital’s patient list
  - Delete existing patient → `deleted=true`
  - Delete non-existent patient → `deleted=false`
  - List patients returns Alice/Bob/Charlie with correct UUID <-> name pairs

### Deletion

- Deleting a hospital does **not** delete patients.
- Deleting a patient does **not** delete hospitals.
- We ensure this by modeling patient <->hospital via a **join/bridge table** for visits (many-to-many).
  Deleting a hospital or patient only removes their **visit** rows, never the other entity.

---

# Special request - Monthly-Bucket Algorithm:

### Overview

Maintain fixed-size **monthly buckets** per hospital and sex to answer:

> “Average age of visitors per month, by sex, for the last 10 years”  
> in predictable, sub-200 ms time.

## Data Stored

For hospital `H`, calendar month `M`, and sex `S`, store:

- **Key:** `(hospitalId=H, year, month, sex=S)`
- **Value:** `(visits_count, age_days_sum)`

Where:

- `visits_count` = number of visits in that bucket.
- `age_days_sum` = sum of `daysBetween(patientDOB, visitDate)` over visits in the bucket.

## Update Operation (Per Visit)

1. Compute `bucketMonth = floorMonth(visitDate)`.
2. Compute `ageDays = daysBetween(DOB, visitDate)`.
3. Upsert at key `(H, bucketMonth, S)`:
   - `visits_count += 1`
   - `age_days_sum += ageDays`

Expected constant time with a hash map; effectively constant with a DB.

## Query Operation (Last 10 Years) - O(120 × |S|) ≈ O(1)

Given `hospitalId` and “now”:

1. Determine a 120-month window (see “Granularity” below).
2. For each month in the window and each sex `S`, fetch the bucket (if present).
3. For each fetched bucket:
   '''
   avg_age_years = (age_days_sum / visits_count) / 365.2425
   '''

At most `120 × |S|` lookups (<= 480), which is effectively constant.

### Complexity

- **Time (update):** O(1) per visit.
- **Time (query):** O(120 × |S|) ~ O(1).
- **Space:** O(#hospitals × 120 × |S|) ~ O(1) (if each hospital only stores their own instance -> |H| = 1).

### Retention

To keep storage bounded:

- Scheduled job deletes raw `Visit` rows older than 10 years.
- Queries always filter to the last 120 months.

## Mid-Month Granularity Issue

Buckets are **monthly**, so mid-month requests raise a policy choice:

1. **Last 120 full months (recommended):**  
   End at the **previous full month** (e.g., querying on 2025-12-15 returns up to **2025-11**).  
   -> no partial months, clean comparability.

2. **Include the current (partial) month:**  
   End at the **current month** (e.g., includes **2025-12**, but only days 1–15).  
   -> the latest point is **under-counted** relative to full months.

Because storage is monthly, exact "to-the-day" accuracy not possible, without changing granularity (length of bucket accumulation).
