
/* =====================================================
   MOBILE MENU
===================================================== */

const mobileMenuButton =
    document.getElementById("mobileMenuButton");

const mobileMenu =
    document.getElementById("mobileMenu");


mobileMenuButton.addEventListener("click", () => {

    mobileMenu.classList.toggle("hidden");

});



/* =====================================================
   SERVICE IMAGE SLIDER
===================================================== */


const serviceImages = {

    carpenter: [
        "https://images.unsplash.com/photo-1601058268499-e52658b8bb88?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1610701596007-11502861dcfa?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1558997519-83ea9252edf8?auto=format&fit=crop&w=900&q=80"
    ],

    electrician: [
        "https://images.unsplash.com/photo-1621905252507-b35492cc74b4?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1555963966-b7ae5404b6ed?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1621905251918-48416bd8575a?auto=format&fit=crop&w=900&q=80"
    ],

    plumber: [
        "https://images.unsplash.com/photo-1607472586893-edb57bdc0e39?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1585704032915-c3400ca199e7?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=900&q=80"
    ],

    painter: [
        "https://images.unsplash.com/photo-1562259949-e8e7689d7828?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1595814433015-e7dbc3e3e7d2?auto=format&fit=crop&w=900&q=80"
    ],

    cleaning: [
        "https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1585421514738-01798e348b17?auto=format&fit=crop&w=900&q=80"
    ],

    appliance: [
        "https://images.unsplash.com/photo-1581094794329-c8112a89af12?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?auto=format&fit=crop&w=900&q=80",
        "https://images.unsplash.com/photo-1626806787461-102c1bfaaea1?auto=format&fit=crop&w=900&q=80"
    ]

};



/* =====================================================
   CHANGE IMAGES EVERY 3 SECONDS
===================================================== */

const imageIndexes = {};


// Initialize indexes

Object.keys(serviceImages).forEach(service => {

    imageIndexes[service] = 0;

});


// Change image

function changeServiceImages() {

    const images =
        document.querySelectorAll(".service-image");


    images.forEach(image => {

        const service =
            image.dataset.service;

        const serviceImageList =
            serviceImages[service];


        if (!serviceImageList) {
            return;
        }


        // Fade out

        image.style.opacity = "0";


        setTimeout(() => {

            imageIndexes[service]++;

            if (
                imageIndexes[service] >=
                serviceImageList.length
            ) {

                imageIndexes[service] = 0;

            }


            image.src =
                serviceImageList[
                    imageIndexes[service]
                ];


            // Fade in

            image.style.opacity = "1";

        }, 300);

    });

}


// Change images every 3 seconds

setInterval(changeServiceImages, 3000);