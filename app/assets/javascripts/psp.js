// initialise GovUK lib
GOVUKFrontend.initAll();
if (document.querySelector('#country') != null) {
    console.log(document.getElementById('country'))
    accessibleAutocomplete({
        element: document.getElementById('country'),
        id: 'country',
        source: '/pension-scheme-practitioner/assets/javascripts/autocomplete/location-autocomplete-graph.json'
    })

    accessibleAutocomplete.enhanceSelectElement({
        defaultValue: '',
        selectElement: document.querySelector('#country')
    })
}

var backLink = document.querySelector('.govuk-back-link');
if (backLink) {
    backLink.classList.remove('js-visible');
    backLink.addEventListener('click', function (e) {
        e.preventDefault();
        if (window.history && window.history.back && typeof window.history.back === 'function') {
            window.history.back();
        }
    });
}

var printLink = document.querySelector('.print-this-page');
if (printLink) {
    printLink.addEventListener('click', function (e) {
        window.print();
        return false;
    });
}
