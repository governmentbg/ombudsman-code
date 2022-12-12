<div class="row m-calendar">

    @foreach ($calendar as $cal)
        <div class="col-12 col-sm-6 col-lg-2 col-md-2">
            {{-- {{ dd($cal) }} --}}
            <div class="m-year">
                {{ $cal['t_year'] }}
            </div>

            <ul class="m-list">
                @foreach ($cal['t_month_list'] as $ml)
                    <li>
                        <a href="?year={{ $cal['t_year'] }}&amp;month={{ $ml->t_month }}#calendar">
                            {{ m2l(App::getLocale(), date('F', mktime(0, 0, 0, $ml->t_month, 10))) }} </a>

                        {{-- {{ $ml->t_month }} --}}
                        {{-- {{ $ml->t_month }} --}}


                    </li>
                @endforeach

        </div>
    @endforeach
</div>
