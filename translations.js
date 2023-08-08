// Charger les traductions depuis le fichier JSON
async function loadTranslations(language) {
    const response = await fetch(`translations/${language}.json`);
    const translations = await response.json();
    return translations;
}


let currentLanguage = (navigator.language === "fr-FR") ? "fr" : "en";
let translations;

// Fonction pour traduire le texte
function translate(key) {
    return translations[key] || key;
}

// Fonction pour afficher les traductions dans le HTML
function displayTranslations() {
    const elementsToTranslate = document.querySelectorAll('[data-translate]');
    elementsToTranslate.forEach(element => {
        const translationKey = element.getAttribute('data-translate');
        if (translationKey === "target_language") {
            let flag_path = `imgs/icons/flag_${translate(translationKey)}.png`;
            element.src = flag_path;
        } else {
            element.textContent = translate(translationKey);
        }
    });
}

// Fonction pour basculer entre les langues
async function toggleLanguage() {
    currentLanguage = (currentLanguage === "en") ? "fr" : "en";
    translations = await loadTranslations(currentLanguage);
    displayTranslations();
}

// Charger les traductions et afficher les traductions initiales
async function init() {
    translations = await loadTranslations(currentLanguage);
    displayTranslations();
}

// Appel de la fonction d'initialisation
init();
