import sbt.Setting
import scoverage.ScoverageKeys

class CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    ".*(BuildInfo|Routes).*",
    ".*config.*",
    ".*modules.*",
    ".*view.*",
    ".*handlers.*",
    ".*repositories.*"
  )

  val excludeFiles: Seq[String] = Seq(
    ".*BuildInfo.*",
    ".*javascript.*",
    ".*Routes.*",
    ".*GuiceInjector",
    ".*ControllerConfiguration",
    ".*TestController",
    ".*PSPDeregistration*.",
    ".*PSPEnrolment*.",
    ".*CacheConnector*.",
    ".*AgentCannotRegisterController",
    ".*AssistantNoAccessController",
    ".*LoginController",
    ".*NeedAnOrganisationAccountController",
    ".*SessionExpiredController",
    ".*LanguageSwitchController",
    ".*DeclarationController",
    ".*CannotDeregisterController",
    ".*NonUKPractitionerController",
    ".*OutsideEuEeaController",
    ".*SessionExpiredController",
    ".*PSPDeregistrationEmail",
    ".*UnauthorisedController",
    ".*ErrorHandlingController",
    ".*FormErrorHelper",
    ".*NonUKAddressFormProvider",
    ".*RegisteredAddressFormProvider",
    ".*CustomBindMapping",
    ".*Address",
    ".*Field",
    ".*MinimalPSP",
    ".*AddressChange",
    ".*NameChange",
    ".*Page",
    ".*ValidationPage",
    ".*ConfirmDeregistrationPage",
    ".*CountryOptionsEUAndEEA",




  )
  def apply(): Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageExcludedPackages:=  excludedPackages.mkString(";"),
    ScoverageKeys.coverageExcludedFiles := excludeFiles.mkString(";")
  )

}
