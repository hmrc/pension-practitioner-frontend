package controllers

import config.FrontendAppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import forms.$className$FormProvider
import javax.inject.Inject
import models.{$className$, GenericViewModel, Mode}
import navigators.CompoundNavigator
import pages.$className$Page
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.{ExecutionContext, Future}

class $className$Controller @Inject()(override val messagesApi: MessagesApi,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      navigator: CompoundNavigator,
                                      identify: IdentifierAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: $className$FormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      config: FrontendAppConfig,
                                      renderer: Renderer
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with NunjucksSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData()andThen requireData).async {
    implicit request =>
      DataRetrievals.retrieveCompanyName { pspName =>

        val preparedForm = request.userAnswers.get($className$Page) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        def viewModel = GenericViewModel(
          submitUrl = routes.$className$Controller.onSubmit(mode).url,
          pspName = pspName)

        val json = Json.obj(
          "form" -> preparedForm,
          "viewModel" -> viewModel,
          "checkboxes" -> $className$.checkboxes(preparedForm)
        )

        renderer.render("$className;format="decap"$.njk", json).map(Ok(_))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData()andThen requireData).async {
    implicit request =>
      DataRetrievals.retrieveCompanyName { pspName =>

      form.bindFromRequest().fold(
        formWithErrors => {

          def viewModel = GenericViewModel(
            submitUrl = routes.$className$Controller.onSubmit(mode).url,
            pspName = pspName)

          val json = Json.obj(
            "form" -> formWithErrors,
            "viewModel"  -> viewModel,
            "checkboxes" -> $className$.checkboxes(formWithErrors)
          )

          renderer.render("$className;format="decap"$.njk", json).map(BadRequest(_))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page, value))
            _ <- userAnswersCacheConnector.save( updatedAnswers.data)
          } yield Redirect(navigator.nextPage($className$Page, mode, updatedAnswers))
      )
    }
  }
}
