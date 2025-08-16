# Hospital gRPC Service — Implementation Notes

Implements the brief with gRPC endpoints to:

- Create/modify/delete hospitals
- Create/modify/delete patients
- Register a patient in a hospital
- List patients of a hospital
- List hospitals of a patient

Duplicate names are allowed; identity is by UUID.

---

## Run

- Build & start: `./gradlew bootRun` (gRPC on port 9090)

---

## Tests

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

---

## Deletion semantics

- Deleting a hospital does **not** delete patients.
- Deleting a patient does **not** delete hospitals.
- We ensure this by modeling patient <->hospital via a **join/bridge table** for visits (many-to-many).
  Deleting a hospital or patient only removes their **visit** rows, never the other entity.
