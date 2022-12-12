{{-- <div class="row"> --}}
<div class="col">
    <h2 class="m-home-title mt-4 mr-5">{{ trans('common.positions_t') }}</h2>


    {{-- </div> --}}
    {{-- <h2 class="m-home-title mt-4 mr-5">{{ trans('common.box_position') }}</h2> --}}

    <div class="m-position">
        <ul class="list">

            @if(App::getLocale()=='bg')

            @foreach (\App\Http\Controllers\MPositionController::pstList(5, 1) as $ps)
            <li>
                <div class="content">
                    <a href="/storage{{ $ps->Pst_file }}" download="">
                        {{ $ps->Pst_name }}
                    </a>
                </div>

                <div class="m-d"></div>

            </li>
            @endforeach

            @endif
        </ul>
        <div class="link-m mb-5"> <a href="/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 15) }}">
                {{ trans('common.all_positions') }} <i class="cil-arrow-right"></i> </a>
        </div>
    </div>

    <h2 class="m-home-title mt-4 mr-5">{{ trans('common.requests_sc') }}</h2>


    <div class="m-position">
        <ul class="list">
            @if(App::getLocale()=='bg')
            @foreach (\App\Http\Controllers\MPositionController::pstList(5, 2) as $ps)
            <li>
                <div class="content">
                    <a href="/storage{{ $ps->Pst_file }}" download="">
                        {{ $ps->Pst_name }}
                    </a>
                </div>

                <div class="m-d"></div>

            </li>
            @endforeach

            @endif
        </ul>
        <div class="link-m mb-5"> <a href="/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 51) }}">{{ trans('common.all_requests_sc') }}
                <i class="cil-arrow-right"></i>
            </a>
        </div>
    </div>

</div>