<div class="clearfix"></div>
@if (Request::segment(1) != 'prava-na-deteto')
    <div class="m-h-4 d-low">
        <div class="container">
            <div class="row justify-content-md-center h-links @if (Request::segment(2)) ip @endif">
                <div class="col col-lg-6 h-border ">
                    <a href="/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 49) }}">
                        <div class="d-flex flex-row hl-1">
                            <div class="h-img">
                                <img src="/img/online_claim.svg" alt="" class="img-fluid" />
                            </div>

                            @php
                                $data = getConfigByKey(2, App::getLocale(), null, 4);
                                
                            @endphp
                            {{-- {{ dd($data) }} --}}

                            <div class="h-text">
                                <div class="h-title">
                                    {{ $data->CfL_name }}
                                    {{-- {{ trans('common.submit_claim') }} --}}
                                </div>


                                <div class="h-body">
                                    {{ $data->CfL_value }}

                                </div>
                            </div>

                        </div>
                    </a>
                </div>

                <div class="col col-lg-6">

                    <a href="/prava-na-deteto/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 39) }}">

                        <div class="d-flex flex-row">
                            <div class="h-img"><img src="/img/online_claim_child.svg" alt=""
                                    class="img-fluid" />
                            </div>
                            @php
                                $data = getConfigByKey(2, App::getLocale(), null, 5);
                                
                            @endphp
                            <div class="h-text">
                                <div class="h-title">
                                    {{ $data->CfL_name }}
                                    {{-- {{ trans('common.submit_claim_c') }} --}}
                                </div>



                                <div class="h-body">
                                    {{ $data->CfL_value }}</div>
                            </div>

                        </div>
                    </a>
                </div>
            </div>

        </div>

    </div>
    {{-- <div class="m-h-3 d-low">
                <div class="container">
                    <div class="row p-2">
                        <div class="col-4">
                           
                        </div>
                        <div class="col text-right">
                            <span class="claim mr-2">
                                <a href="/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 49) }}">
                                    {{ trans('common.submit_claim') }}</a>
                            </span>
                            <span class="claim">
                                <a
                                    href="/prava-na-deteto/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 39) }}">
                                    {{ trans('common.submit_claim_c') }}</a>
                            </span>
                        </div>
                    </div>
                </div>

            </div> --}}
@endif
