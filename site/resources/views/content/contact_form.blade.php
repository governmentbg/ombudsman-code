<div class="row">
    <div class="col">
        <h5>{{ $msg }}</h5>
    </div>
</div>
<ul class="list-unstyled">
    @foreach ($error as $err)
        <li> <i class="cis-x text-danger"> </i> {{ $err }}</li>
    @endforeach
</ul>
<div class="row">
    <div class="col-sm-12 col-xs-12 col-md-6 col-lg-6 col-xl-6">
        <div class="m-card p-3">
            <div class="m-form form">
                <form method="POST" action="/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 42) }}">
                    @csrf
                    <div class="row">


                    </div>
                    <div class="row">

                        <div class="col-sm-12 col-xs-12 col-lg-6 col-xl-6 col-md-6">
                            <div class="form-group">
                                <label for="name">{{ trans('common.claim.name') }}:</label>
                                <input type="" class="form-control" name="name" placeholder=""  title="{{ trans('common.claim.name') }}" aria-label="{{ trans('common.claim.name') }}"
                                    value="{{ $req['name'] }}">
                            </div>
                        </div>
                        <div class="col-sm-12 col-xs-12 col-lg-6 col-xl-6 col-md-6">
                            <div class="form-group">
                                <label for="family">{{ trans('common.claim.family') }}:</label>
                                <input type="" class="form-control" name="family" placeholder=""  title="{{ trans('common.claim.family') }}" aria-label="{{ trans('common.claim.family') }}"
                                    value="{{ $req['family'] }}">
                            </div>
                        </div>



                    </div>



                    <div class="row">

                        <div class="col-sm-12 col-xs-12 col-lg-6 col-xl-6 col-md-6">
                            <div class="form-group">
                                <label for="phone">{{ trans('common.claim.phone') }}:</label>
                                <input type="" class="form-control" name="phone" placeholder=""
                                    value="{{ $req['phone'] }}">
                            </div>

                        </div>
                        <div class="col-sm-12 col-xs-12 col-lg-6 col-xl-6 col-md-6">
                            <div class="form-group">
                                <label for="email">E-mail:</label>
                                <input type="" class="form-control" name="email" placeholder=""  title="E-mail" aria-label="E-mail"
                                    value="{{ $req['email'] }}">
                            </div>
                        </div>



                    </div>


                    <div class="row">
                        {{-- {{ dd($req) }} --}}
                        <div class="col-lg-12 col-xl-12 col-md-12 col-sm-12 col-xs-12">
                            <div class="form-group">
                                <label for="request1">{{ trans('common.claim.request') }}:</label>
                                <textarea class="form-control ta" rows="4" id="request1" name="request1"  title="{{ trans('common.claim.request') }}" aria-label="{{ trans('common.claim.request') }}">{{ $req['request1'] }}</textarea>
                            </div>
                        </div>
                    </div>







                    <div class="row">
                        <div class="col-12 text-right">
                            <button type="submit" title="{{ trans('common.claim.sendContact') }}" class="btn m-btn">{{ trans('common.claim.sendContact') }}</button>
                        </div>


                    </div>


                </form>

            </div>

        </div>
    </div>
    <div class="col-sm-12 col-xs-12 col-md-6 col-lg-6 col-xl-6">
        <div class="mapouter">
            <div class="gmap_canvas"><iframe frameborder="0" height="300" id="gmap_canvas" scrolling="no"
                    src="https://maps.google.com/maps?q=%D0%A1%D0%BE%D1%84%D0%B8%D1%8F%201202,%20%D1%83%D0%BB.%20%D0%93%D0%B5%D0%BE%D1%80%D0%B3%20%D0%92%D0%B0%D1%88%D0%B8%D0%BD%D0%B3%D1%82%D0%BE%D0%BD%20%E2%84%96%2022&amp;t=&amp;z=19&amp;ie=UTF8&amp;iwloc=&amp;output=embed"
                    width="100%"></iframe></div>
        </div>
        {{-- <div id="map"></div>

        <script type="text/javascript">
            function initMap() {

                const myLatLng = {
                    lat: 22.2734719,
                    lng: 70.7512559
                };

                const map = new google.maps.Map(document.getElementById("map"), {

                    zoom: 5,

                    center: myLatLng,

                });



                new google.maps.Marker({

                    position: myLatLng,

                    map,

                    title: "Hello Rajkot!",

                });

            }



            window.initMap = initMap;
        </script> --}}

        {{-- {{ env('GOOGLE_MAP_KEY') }} --}}

        {{-- <script type="text/javascript"
            src="https://maps.google.com/maps/api/js?key=AIzaSyBXcRazJIu6cyAXvV_EyGa77fDPuDogMvQ&callback=initMap"></script> --}}

    </div>
</div>
