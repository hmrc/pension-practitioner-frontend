package pages

import pages.behaviours.PageBehaviours

class TestPagePageSpec extends PageBehaviours {

  "TestPagePage" - {

    beRetrievable[Boolean](TestPagePage)

    beSettable[Boolean](TestPagePage)

    beRemovable[Boolean](TestPagePage)
  }
}
