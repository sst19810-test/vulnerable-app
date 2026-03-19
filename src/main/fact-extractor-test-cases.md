# Fact Extractor Test Cases — Logical Coverage Map

This is a test plan for the uploaded Java fact extractor.

I organized it in the same order the extractor itself works:

1. **Scan + project discovery**
2. **File / package / import facts**
3. **Type + member declaration facts**
4. **Body AST facts**
5. **Binding facts**
6. **Annotation facts**
7. **Failure / regression / edge cases**

## Legend

- **Status = Verified**: I spot-ran a tiny repro against the current implementation.
- **Status = Inferred**: derived from the code path and should be covered.
- **Status = Red flag**: current implementation looks broken or incomplete for this scenario.

## Recommended execution order

Run these in this order so failures are easier to localize:

- **Smoke**: SCN-01 to SCN-04
- **Declarations**: HDR-01 to MEM-09
- **Bodies**: AST-01 to AST-12
- **Bindings**: BND-01 to BND-12
- **Annotations**: ANN-01 to ANN-10
- **Regressions / gaps**: GAP-01 to GAP-10

---

## 1) Scan / project discovery / metadata

| ID | Scenario | Minimal setup | Assert these facts / outcomes | Status |
|---|---|---|---|---|
| SCN-01 | Single valid Java file | one `A.java` | `scan_start`, `scan_done`, `files=1`, `folders>0`, `numlines=1` | Verified |
| SCN-02 | Empty project | no `.java` files | `scan_start`, `scan_done`, `files=0`, no declaration/body facts | Inferred |
| SCN-03 | Nested folder tree | `src/main/java/p/q/A.java` | `folders`, `files`, `containerparent` chain is emitted correctly | Verified |
| SCN-04 | Mixed valid + invalid Java files | one parseable file + one broken file | valid file facts still emitted; `scan_done.filesFailed` increments | Inferred |
| SCN-05 | No build file present | plain source folder, no `pom.xml` / `build.gradle` | scan still runs, but bindings/types degrade gracefully | Inferred |
| SCN-06 | Maven classpath resolution | project with `pom.xml` | classpath is auto-resolved before scan | Inferred |
| SCN-07 | Gradle classpath resolution | project with `build.gradle` or `build.gradle.kts` | classpath is auto-resolved before scan | Inferred |
| SCN-08 | Batch flush threshold | set low `maxRowsPerBatch` | multiple `FactBatch` chunks per relation; row totals still correct | Inferred |

---

## 2) Header facts: package + imports

| ID | Scenario | Minimal setup | Assert these relations | Status |
|---|---|---|---|---|
| HDR-01 | Package declaration present | `package p; class A {}` | `packages`, `cupackage` | Verified |
| HDR-02 | Default package | `class A {}` | empty-package entry still handled via `packages` + `cupackage` | Inferred |
| HDR-03 | Normal import | `import java.util.List;` | `imports.kind = 0` | Inferred |
| HDR-04 | Wildcard import | `import java.util.*;` | `imports.kind = 1` | Inferred |
| HDR-05 | Static import | `import static java.util.Collections.emptyList;` | `imports.kind = 2` | Inferred |
| HDR-06 | Static wildcard import | `import static java.util.Collections.*;` | `imports.kind = 3` | Inferred |

---

## 3) Type declaration facts

| ID | Scenario | Minimal setup | Assert these relations | Status |
|---|---|---|---|---|
| TYP-01 | Simple class | `class A {}` | `classes_or_interfaces=1` | Verified |
| TYP-02 | Interface | `interface A {}` | `classes_or_interfaces`, `isInterface` | Inferred |
| TYP-03 | Enum type marker | `enum E { A, B }` | `classes_or_interfaces`, `isEnumType` | Verified |
| TYP-04 | Annotation type marker | `@interface X {}` | `classes_or_interfaces`, `isAnnotType` | Verified |
| TYP-05 | Nested type | `class A { class B {} }` | parent + child in `classes_or_interfaces`, `enclInReftype` | Inferred |
| TYP-06 | Extends class | `class B extends A {}` | `extendsReftype` | Inferred |
| TYP-07 | Implements interface | `class B implements I {}` | `implInterface` | Inferred |
| TYP-08 | Multiple interfaces | `class B implements I1, I2 {}` | multiple `implInterface` rows | Inferred |
| TYP-09 | Type modifiers | `public abstract class A {}` | `modifiers`, `hasModifier` | Inferred |
| TYP-10 | Sealed / non-sealed / final type modifiers | JDK 17+ syntax | correct modifier rows | Inferred |
| TYP-11 | Record declaration | `record R(int a) {}` | should emit `classes_or_interfaces`, `isRecord`, record component fields | **Red flag** |

### Notes on TYP-11

Spot run shows **record files are currently not parsing** in this implementation, even though there is record-specific code in `TypeDeclarationPass` and `MemberDeclarationPass`.

---

## 4) Member declaration facts

| ID | Scenario | Minimal setup | Assert these relations | Status |
|---|---|---|---|---|
| MEM-01 | Single field | `int x;` | `fields` | Verified |
| MEM-02 | Multi-variable field declaration | `int x, y;` | two `fields` rows | Inferred |
| MEM-03 | Method with return + params | `int f(String s)` | `methods`, `params`, `paramName` | Verified |
| MEM-04 | Explicit constructor | `A() {}` | `constrs` | Verified |
| MEM-05 | Overloaded methods | `f()`, `f(int)` | two `methods`, distinct signatures | Inferred |
| MEM-06 | Overloaded constructors | `A()`, `A(int)` | two `constrs`, distinct signatures | Verified |
| MEM-07 | Static initializer block | `static { ... }` | synthetic `methods` row named `<clinit>` | Inferred |
| MEM-08 | Instance initializer block | `{ ... }` | synthetic `methods` row named `<init_block>` | Inferred |
| MEM-09 | Parameter modifiers | `void f(final int x)` | `hasModifier` on param | Inferred |
| MEM-10 | Record components become fields | `record R(int a, String b)` | `fields` rows for components, plus implicit `private` + `final` modifiers | **Red flag** |
| MEM-11 | Implicit default constructor | `class A {}` | synthetic `constrs` row for `A()` | **Red flag** |
| MEM-12 | Annotation type members | `@interface X { String value(); }` | method-like facts for annotation members | **Red flag** |
| MEM-13 | Enum constants as members | `enum E { A, B }` | enum entries represented somehow (usually field-ish or dedicated model) | **Red flag** |

### Notes on member red flags

- **MEM-11 verified broken**: `new A()` binds correctly **only** when `A()` is explicit. With implicit default constructor, current scan emits **no `constrs` row** and constructor binding is missing.
- **MEM-12 verified broken**: annotation members are not emitted because `AnnotationMemberDeclaration` is not handled in `MemberDeclarationPass`.
- **MEM-13 verified broken**: enum type marker is emitted, but enum constants themselves are not emitted as facts.

---

## 5) Body AST facts

| ID | Scenario | Minimal setup | Assert these relations | Status |
|---|---|---|---|---|
| AST-01 | Basic method block | `void f(){}` | root `stmts` row for method body | Verified |
| AST-02 | Expression statement | `x = 1;` | `stmts(kind=14)` + expr subtree | Verified |
| AST-03 | Local variable declaration | `int b = a + x;` | `localvars`, `varInit`, `exprs` | Verified |
| AST-04 | Return statement with expr | `return b;` | `returnExpr` + `exprs` | Verified |
| AST-05 | Assignment expression | `x = y;` | `assignment` row linking assign/target/value | Inferred |
| AST-06 | Switch statement / switch expression | `switch(x){...}` | synthetic case stmt rows (`kind=21`), nested exprs | Verified |
| AST-07 | Catch clause | `try { ... } catch (E e) { ... }` | synthetic catch stmt row (`kind=22`) | Verified |
| AST-08 | Lambda with block body | `Runnable r = () -> { ... };` | lambda expr + nested stmt subtree | Inferred |
| AST-09 | Field initializer expression tree | `int x = 1;` | synthetic field-init stmt + `exprs` + `varInit` | Verified |
| AST-10 | Method reference expr | `Runnable r = this::g;` | `exprs(kind=15)` | Verified |
| AST-11 | Explicit constructor invocation stmt | `this(1);` / `super(1);` | stmt row with `kind=19` | Verified |
| AST-12 | Local class declaration inside method | `class L {}` inside body | stmt row with `kind=18` at AST level | Inferred |
| AST-13 | Text block literal | `"""hello"""` | `stringLiterals` | Inferred |
| AST-14 | String literal | `"x"` | `stringLiterals` | Verified |
| AST-15 | Int literal | `1` | `intLiterals` | Verified |
| AST-16 | Boolean literal | `true` | `boolLiterals` | Inferred |
| AST-17 | Long literal | `1L` | `longLiterals` | Inferred |
| AST-18 | Double literal | `1.5` | `doubleLiterals` | Inferred |
| AST-19 | Char literal | `'c'` | `charLiterals` | Inferred |
| AST-20 | Null literal | `null` | `nullLiterals` | Inferred |

---

## 6) Binding facts

| ID | Scenario | Minimal setup | Assert these relations | Status |
|---|---|---|---|---|
| BND-01 | Local var read binding | `return b;` | `variableBinding(expr -> localvars)` | Verified |
| BND-02 | Param read binding | `b = a + 1;` | `variableBinding(expr -> params)` | Verified |
| BND-03 | Field read binding | `b = a + x;` | `variableBinding(expr -> fields)` | Verified |
| BND-04 | Same-file explicit ctor binding | `A(){}` + `new A()` | `callableBinding(expr -> constrs)` | Verified |
| BND-05 | Same-file method call binding | `greet();` | `callableBinding(expr -> methods)` | Verified |
| BND-06 | Cross-file method call binding | class `B` calls method in class `A` | `callableBinding` across files | Verified |
| BND-07 | Cross-file explicit ctor binding | class `B` does `new A()` and `A()` is explicit | `callableBinding` across files | Inferred |
| BND-08 | Call receiver binding | `a.greet()` | `callReceiver(callExpr -> receiverExpr)` | Verified |
| BND-09 | Call arg binding | `foo(x, y)` | one `callArg` row per arg index | Inferred |
| BND-10 | Method reference binding | `this::g` | `callableBinding(expr -> methods)` | Verified |
| BND-11 | Assignment decomposition | `x = y` | `assignment(assignExpr,target,value)` | Inferred |
| BND-12 | External field stub binding | `Integer.MAX_VALUE` | `fields` stub + `variableBinding` | Inferred |
| BND-13 | External method stub binding | `list.add("x")` with resolvable classpath | stub `methods` + `callableBinding` | Inferred |
| BND-14 | External ctor stub binding | `new ArrayList<>()` with resolvable classpath | stub `constrs` + `callableBinding` | Inferred |
| BND-15 | Unresolvable external symbol skipped | missing third-party lib | scan continues; unresolved external is not fatal | Verified-ish |
| BND-16 | In-project callable missing from index | weird mismatch / signature drift | increments binding-failed metric | Inferred |
| BND-17 | Implicit default constructor binding | `class A {}` + `new A()` | should bind to synthetic ctor | **Red flag** |
| BND-18 | Explicit constructor invocation binding | `this(1)` / `super(1)` | should link to target ctor | **Red flag** |

### Notes on binding red flags

- **BND-17 verified broken**: implicit default constructors are not emitted, so `new A()` produces no `callableBinding` unless `A()` is explicitly declared.
- **BND-18 verified broken**: `this(...)` / `super(...)` show up as stmt kind `19`, but `BindingPass` does not bind `ExplicitConstructorInvocationStmt`.

---

## 7) Annotation facts

| ID | Scenario | Minimal setup | Assert these relations | Status |
|---|---|---|---|---|
| ANN-01 | Type annotation | `@Ann class A {}` | `annotationUse(target_kind=TYPE)` | Verified |
| ANN-02 | Field annotation | `@Ann String f;` | `annotationUse(target_kind=FIELD)` | Verified |
| ANN-03 | Method annotation | `@Ann void f(){}` | `annotationUse(target_kind=METHOD)` | Verified |
| ANN-04 | Param annotation | `void f(@Ann String p){}` | `annotationUse(target_kind=PARAM)` | Verified |
| ANN-05 | Constructor annotation | `@Ann A(){}` | `annotationUse(target_kind=CTOR)` | Inferred |
| ANN-06 | Marker annotation | `@Deprecated` | `annotationUse` only, no `annotationAttr` | Inferred |
| ANN-07 | Single-member annotation | `@Ann("x")` | `annotationAttr(attr_name=value, attr_value=x)` | Verified |
| ANN-08 | Normal annotation with multiple attrs | `@Ann(value="x", n=1)` | one `annotationAttr` per pair | Verified |
| ANN-09 | Annotation FQN fallback when unresolved | missing annotation classpath | simple name still emitted as `annotation_fqn` fallback | Inferred |
| ANN-10 | Annotation with array/class/enum attrs | e.g. `@Target({TYPE, METHOD})` | `annotationAttr.attr_value` stores best-effort string form | Verified |
| ANN-11 | Local variable annotation | annotated local declaration | expect `annotationUse(target_kind=LOCAL_VAR)` | Inferred / worth checking carefully |

### Note on ANN-11

This is worth a dedicated regression because the parent-node logic in `AnnotationPass` is a little delicate for locals vs fields.

---

## 8) Gap / regression suite (run every time)

These are the cases most likely to catch real breakage in this codebase.

| ID | Scenario | Why it matters | Current expectation |
|---|---|---|---|
| GAP-01 | `class A {}` + `new A()` | catches missing implicit default ctor emission | **Currently fails** |
| GAP-02 | Cross-file `new A()` where `A` has no explicit ctor | same bug, but across files | **Currently fails** |
| GAP-03 | `record R(int a) {}` | record support should exist but parser currently rejects it | **Currently fails** |
| GAP-04 | `enum E { A, B }` | enum constants not modeled | **Currently incomplete** |
| GAP-05 | `@interface X { String value(); }` | annotation members are ignored | **Currently fails** |
| GAP-06 | `A(){ this(1); }` | explicit constructor invocation not bound | **Currently fails** |
| GAP-07 | local class inside method | AST stmt may exist, but no type facts are emitted | **Likely incomplete** |
| GAP-08 | anonymous class body | type/member modeling likely incomplete | **Likely incomplete** |
| GAP-09 | pattern matching (`instanceof String s`, record patterns, switch patterns) | pattern-node support is commented out | **Likely incomplete** |
| GAP-10 | external library bindings with partial classpath | tells you whether stub strategy is actually working | **Must be regression-tested** |

---

## 9) Minimal “golden” smoke suite

If you only want a lean but high-signal suite first, start here:

1. **GOLD-01 — Basic method body**
   - `class A { int x = 1; int f(int a){ int b = a + x; return b; } }`
   - Assert: `fields`, `methods`, `params`, `localvars`, `exprs`, `stmts`, `varInit`, `returnExpr`, `variableBinding`

2. **GOLD-02 — Cross-file method call**
   - `A.greet()` called from `B.run()`
   - Assert: `callableBinding`, `callReceiver`, cross-file declaration facts

3. **GOLD-03 — Explicit constructor binding**
   - `class A { A(){} void f(){ new A(); } }`
   - Assert: `constrs`, `callableBinding`

4. **GOLD-04 — Annotation extraction**
   - custom annotation on type, field, method, param
   - Assert: `annotationUse`, `annotationAttr`

5. **GOLD-05 — Switch + catch**
   - `try/switch/catch` body
   - Assert: synthetic `stmts` for switch entries and catch clause, plus `returnExpr`

6. **GOLD-06 — Known bug guard: implicit default ctor**
   - `class A { void f(){ new A(); } }`
   - Assert: should emit/bind ctor; currently does **not**

7. **GOLD-07 — Known bug guard: record**
   - `record R(int a) {}`
   - Assert: should parse and emit record facts; currently does **not**

8. **GOLD-08 — Known bug guard: annotation type member**
   - `@interface X { String value(); }`
   - Assert: should emit member facts; currently does **not**

---

## 10) Suggested assertion style

For each test, assert at **three levels**:

1. **Presence**
   - relation exists at all (`methods`, `fields`, `callableBinding`, etc.)

2. **Cardinality**
   - exact row count when the input is tiny and deterministic

3. **Wiring**
   - parent/child linkage is correct
   - examples:
     - `params.parentid == methods.id`
     - `returnExpr.callable == methods.id`
     - `callableBinding.callerid == exprs.id`
     - `varInit.variable == fields.id / localvars.id`

For this extractor, **wiring assertions are way more valuable than raw row counts**. Raw counts catch regressions; wiring catches incorrect semantics.

---

## 11) What I would prioritize fixing first

1. **Implicit default constructor emission + binding**
2. **Record parsing / record support**
3. **Annotation member declarations**
4. **Explicit constructor invocation binding (`this` / `super`)**
5. **Enum constant modeling**
6. **Pattern-matching AST coverage**

