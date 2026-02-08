package dev.flowty.bowlby.model;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import com.mastercard.test.flow.validation.AbstractValidator;
import com.mastercard.test.flow.validation.junit5.Validator;

/**
 * Checks the validity of the {@link BowlbySystem} model
 */
@SuppressWarnings("static-method")
class BowlbySystemTest {

  /**
   * @return standard flow-validity test instances
   */
  @TestFactory
  Stream<DynamicNode> checks() {
    return new Validator()
        .checking( BowlbySystem.MODEL )
        .with( AbstractValidator.defaultChecks() )
        .tests();
  }
}
