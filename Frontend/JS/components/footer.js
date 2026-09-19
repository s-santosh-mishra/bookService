/*   ServiceHub Footer */

const currentYear = new Date().getFullYear();

const serviceHubFooter = `

<footer class="w-full border-t border-gray-800 mt-16">

    <div class="max-w-7xl mx-auto px-6 py-8">

        <div class="flex flex-col md:flex-row
                    justify-between items-center gap-4">


            <!-- Company -->

            <div>

                <p class="font-semibold">
                    ServiceHub
                </p>

                <p class="text-sm text-gray-500 mt-1">
                    Trusted Services, Anytime Anywhere
                </p>

            </div>


            <!-- Copyright -->

            <p class="text-sm text-gray-500">

                © ${currentYear} ServiceHub.
                All rights reserved.

            </p>


        </div>

    </div>

</footer>

`;

/*   Insert Footer */

const footerContainer = document.getElementById("serviceHubFooter");

if (footerContainer) {
  footerContainer.innerHTML = serviceHubFooter;
}
