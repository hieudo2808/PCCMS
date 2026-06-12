# Medicine Category API Test Cases

## Pre-report Information
1. **Exact MedicineCategoryController routes and methods:**
   - Base Route: `/api/v1/catalog/medicine-categories` (with global `/api` prefix applied by ApiConfig)
   - `POST /api/v1/catalog/medicine-categories`: `create`
   - `PUT /api/v1/catalog/medicine-categories/{id}`: `update`
   - `GET /api/v1/catalog/medicine-categories/{id}`: `getById`
   - `GET /api/v1/catalog/medicine-categories`: `list` (with optional `activeOnly` boolean query param)
   - `DELETE /api/v1/catalog/medicine-categories/{id}`: `delete`

2. **Request DTO fields:**
   - `MedicineCategoryCreateRequest`: `name` (NotBlank, Max 120), `description` (String), `isActive` (NotNull)
   - `MedicineCategoryUpdateRequest`: `name` (NotBlank, Max 120), `description` (String), `isActive` (NotNull)

3. **Response DTO fields:**
   - `MedicineCategoryResponse`: `id` (UUID), `name` (String), `description` (String), `isActive` (Boolean)

4. **Service methods called:**
   - `MedicineCategoryService.create(MedicineCategoryCreateRequest)`
   - `MedicineCategoryService.update(UUID, MedicineCategoryUpdateRequest)`
   - `MedicineCategoryService.getById(UUID)`
   - `MedicineCategoryService.listActive()` and `MedicineCategoryService.listAll()`
   - `MedicineCategoryService.delete(UUID)`

5. **ErrorCodes thrown for failure paths:**
   - `ERR_MED_009_CATEGORY_NAME_EXISTS` (Create/Update)
   - `ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND` (Update/Get/Delete)
   - `ERR_MED_010_CATEGORY_IN_USE` (Delete)

6. **Proposed testcase ID list:**
   - API-MEDCAT-001: List medicine categories (activeOnly=true)
   - API-MEDCAT-002: List medicine categories (activeOnly=false)
   - API-MEDCAT-003: Get medicine category by ID successfully
   - API-MEDCAT-004: Get medicine category - Not Found
   - API-MEDCAT-005: Create medicine category successfully
   - API-MEDCAT-006: Create medicine category - Name already exists
   - API-MEDCAT-007: Create medicine category - Validation failure
   - API-MEDCAT-008: Update medicine category successfully
   - API-MEDCAT-009: Update medicine category - Validation failure
   - API-MEDCAT-010: Delete medicine category successfully

---

## Test Cases

### API-MEDCAT-001: List medicine categories (activeOnly=true)
- **Module:** Medicine
- **Controller:** MedicineCategoryController
- **Endpoint:** `/api/v1/catalog/medicine-categories`
- **HTTP Method:** GET
- **Test Type:** API Test
- **Tool:** MockMvc
- **Preconditions:** Service mocked to return a list of active categories.
- **Request Params:** `activeOnly=true` (or default omitted)
- **Mocked Service Behavior:** `medicineCategoryService.listActive()` returns `[MedicineCategoryResponse]`
- **Expected HTTP Status:** 200 OK
- **Expected JSON Fields:** `success: true`, `code: 200`, `message: "Thao tác thành công"`, `data: [...]`
- **Automation Target Class:** `MedicineCategoryControllerTest`
- **Automation Target Method:** `should_ListActiveCategories_Successfully`
- **Priority:** P0

### API-MEDCAT-002: List medicine categories (activeOnly=false)
- **Module:** Medicine
- **Controller:** MedicineCategoryController
- **Endpoint:** `/api/v1/catalog/medicine-categories?activeOnly=false`
- **HTTP Method:** GET
- **Test Type:** API Test
- **Tool:** MockMvc
- **Preconditions:** Service mocked to return a list of all categories.
- **Request Params:** `activeOnly=false`
- **Mocked Service Behavior:** `medicineCategoryService.listAll()` returns `[MedicineCategoryResponse]`
- **Expected HTTP Status:** 200 OK
- **Expected JSON Fields:** `success: true`, `code: 200`, `message: "Thao tác thành công"`, `data: [...]`
- **Automation Target Class:** `MedicineCategoryControllerTest`
- **Automation Target Method:** `should_ListAllCategories_When_ActiveOnlyIsFalse`
- **Priority:** P1

### API-MEDCAT-003: Get medicine category by ID successfully
- **Module:** Medicine
- **Controller:** MedicineCategoryController
- **Endpoint:** `/api/v1/catalog/medicine-categories/{id}`
- **HTTP Method:** GET
- **Test Type:** API Test
- **Tool:** MockMvc
- **Preconditions:** Valid UUID path variable.
- **Path Params:** `id`
- **Mocked Service Behavior:** `medicineCategoryService.getById(id)` returns `MedicineCategoryResponse`
- **Expected HTTP Status:** 200 OK
- **Expected JSON Fields:** `success: true`, `code: 200`, `message: "Thao tác thành công"`, `data.id: ...`, `data.name: ...`
- **Automation Target Class:** `MedicineCategoryControllerTest`
- **Automation Target Method:** `should_GetCategoryById_Successfully`
- **Priority:** P0

### API-MEDCAT-004: Get medicine category - Not Found
- **Module:** Medicine
- **Controller:** MedicineCategoryController
- **Endpoint:** `/api/v1/catalog/medicine-categories/{id}`
- **HTTP Method:** GET
- **Test Type:** API Test
- **Tool:** MockMvc
- **Preconditions:** ID does not exist.
- **Path Params:** `id`
- **Mocked Service Behavior:** `medicineCategoryService.getById(id)` throws `BusinessException(ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND)`
- **Expected HTTP Status:** 404 Not Found
- **Expected JSON Fields:** `success: false`, `code: 404`, `errorCode: "ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND"`
- **Automation Target Class:** `MedicineCategoryControllerTest`
- **Automation Target Method:** `should_Return404_When_CategoryNotFound`
- **Priority:** P1

### API-MEDCAT-005: Create medicine category successfully
- **Module:** Medicine
- **Controller:** MedicineCategoryController
- **Endpoint:** `/api/v1/catalog/medicine-categories`
- **HTTP Method:** POST
- **Test Type:** API Test
- **Tool:** MockMvc
- **Preconditions:** Valid request payload.
- **Request Body:** `{ "name": "Antibiotics", "description": "...", "isActive": true }`
- **Mocked Service Behavior:** `medicineCategoryService.create(any())` returns `MedicineCategoryResponse`
- **Expected HTTP Status:** 200 OK (project convention: ApiResponse.created() returns HTTP 200; body code=201)
- **Expected JSON Fields:** `success: true`, `code: 201`, `message: "Thao tác thành công"`, `data.id: ...`, `data.name: "Antibiotics"`
- **Automation Target Class:** `MedicineCategoryControllerTest`
- **Automation Target Method:** `should_CreateCategory_Successfully`
- **Priority:** P0

### API-MEDCAT-006: Create medicine category - Name already exists
- **Module:** Medicine
- **Controller:** MedicineCategoryController
- **Endpoint:** `/api/v1/catalog/medicine-categories`
- **HTTP Method:** POST
- **Test Type:** API Test
- **Tool:** MockMvc
- **Preconditions:** Valid request payload but name exists.
- **Request Body:** `{ "name": "Antibiotics", "description": "...", "isActive": true }`
- **Mocked Service Behavior:** `medicineCategoryService.create(any())` throws `BusinessException(ERR_MED_009_CATEGORY_NAME_EXISTS)` — ERR_MED_009 maps to HTTP 409
- **Expected HTTP Status:** 409 Conflict
- **Expected JSON Fields:** `success: false`, `code: 409`, `errorCode: "ERR_MED_009_CATEGORY_NAME_EXISTS"`
- **Automation Target Class:** `MedicineCategoryControllerTest`
- **Automation Target Method:** `should_Return409_When_CategoryNameExists`
- **Priority:** P1

### API-MEDCAT-007: Create medicine category - Validation failure
- **Module:** Medicine
- **Controller:** MedicineCategoryController
- **Endpoint:** `/api/v1/catalog/medicine-categories`
- **HTTP Method:** POST
- **Test Type:** API Test
- **Tool:** MockMvc
- **Preconditions:** Invalid payload (e.g. blank name, null isActive).
- **Request Body:** Parameterized (see CSV)
- **Mocked Service Behavior:** N/A (should not reach service)
- **Expected HTTP Status:** 400 Bad Request
- **Expected JSON Fields:** `success: false`, `code: 400`, `errorCode: "ERR_400_BAD_REQUEST"`
- **Automation Target Class:** `MedicineCategoryControllerTest`
- **Automation Target Method:** `should_Return400_When_CreateRequestIsInvalid`
- **Priority:** P0

### API-MEDCAT-008: Update medicine category successfully
- **Module:** Medicine
- **Controller:** MedicineCategoryController
- **Endpoint:** `/api/v1/catalog/medicine-categories/{id}`
- **HTTP Method:** PUT
- **Test Type:** API Test
- **Tool:** MockMvc
- **Preconditions:** Valid ID and request payload.
- **Path Params:** `id`
- **Request Body:** `{ "name": "Updated Name", "description": "...", "isActive": false }`
- **Mocked Service Behavior:** `medicineCategoryService.update(eq(id), any())` returns `MedicineCategoryResponse`
- **Expected HTTP Status:** 200 OK
- **Expected JSON Fields:** `success: true`, `code: 200`, `message: "Thao tác thành công"`, `data.name: "Updated Name"`
- **Automation Target Class:** `MedicineCategoryControllerTest`
- **Automation Target Method:** `should_UpdateCategory_Successfully`
- **Priority:** P0

### API-MEDCAT-009: Update medicine category - Validation failure
- **Module:** Medicine
- **Controller:** MedicineCategoryController
- **Endpoint:** `/api/v1/catalog/medicine-categories/{id}`
- **HTTP Method:** PUT
- **Test Type:** API Test
- **Tool:** MockMvc
- **Preconditions:** Invalid payload (e.g. blank name, null isActive).
- **Request Body:** Parameterized (see CSV)
- **Mocked Service Behavior:** N/A (should not reach service)
- **Expected HTTP Status:** 400 Bad Request
- **Expected JSON Fields:** `success: false`, `code: 400`, `errorCode: "ERR_400_BAD_REQUEST"`
- **Automation Target Class:** `MedicineCategoryControllerTest`
- **Automation Target Method:** `should_Return400_When_UpdateRequestIsInvalid`
- **Priority:** P0

### API-MEDCAT-010: Delete medicine category successfully
- **Module:** Medicine
- **Controller:** MedicineCategoryController
- **Endpoint:** `/api/v1/catalog/medicine-categories/{id}`
- **HTTP Method:** DELETE
- **Test Type:** API Test
- **Tool:** MockMvc
- **Preconditions:** Valid ID.
- **Path Params:** `id`
- **Mocked Service Behavior:** `medicineCategoryService.delete(id)` completes successfully
- **Expected HTTP Status:** 200 OK
- **Expected JSON Fields:** `success: true`, `code: 200`, `message: "Thao tác thành công"`, `data: null`
- **Automation Target Class:** `MedicineCategoryControllerTest`
- **Automation Target Method:** `should_DeleteCategory_Successfully`
- **Priority:** P0
