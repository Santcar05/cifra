package cifra;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "cifra", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final Set<String> COMA_FLOTANTE =
            Set.of("double", "float", "java.lang.Double", "java.lang.Float");

    private static final ArchCondition<JavaCodeUnit> NO_USA_COMA_FLOTANTE =
            new ArchCondition<>("no recibir ni devolver double ni float") {
                @Override
                public void check(JavaCodeUnit unidad, ConditionEvents eventos) {
                    var tipos = new ArrayList<JavaClass>(unidad.getRawParameterTypes());
                    tipos.add(unidad.getRawReturnType());
                    for (var tipo : tipos) {
                        if (COMA_FLOTANTE.contains(tipo.getName())) {
                            eventos.add(SimpleConditionEvent.violated(unidad,
                                    unidad.getFullName() + " usa " + tipo.getName()));
                        }
                    }
                }
            };

    @ArchTest
    static final ArchRule ningun_campo_usa_coma_flotante =
            fields().that().areDeclaredInClassesThat().resideInAPackage("cifra..")
                    .should().notHaveRawType(double.class)
                    .andShould().notHaveRawType(float.class)
                    .andShould().notHaveRawType(Double.class)
                    .andShould().notHaveRawType(Float.class)
                    .because("el dinero se representa con BigDecimal, nunca con coma flotante");

    @ArchTest
    static final ArchRule ningun_metodo_usa_coma_flotante =
            codeUnits().that().areDeclaredInClassesThat().resideInAPackage("cifra..")
                    .should(NO_USA_COMA_FLOTANTE)
                    .because("el dinero se representa con BigDecimal, nunca con coma flotante");

    @ArchTest
    static final ArchRule los_controladores_no_exponen_entidades_jpa =
            methods().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .should().notHaveRawReturnType(annotatedWith(Entity.class))
                    .allowEmptyShould(true)
                    .because("la entidad es un detalle de persistencia; la API expone DTO");
}
