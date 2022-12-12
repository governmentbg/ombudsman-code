<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use KossShtukert\LaravelNovaSelect2\Select2;
use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\Boolean;
use Laravel\Nova\Fields\Date;
use Laravel\Nova\Fields\HasMany;
use Laravel\Nova\Fields\Hidden;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Image;
use Laravel\Nova\Fields\Select;
use Laravel\Nova\Fields\Text;
use Laravel\Nova\Http\Requests\NovaRequest;

class MOmbudsman extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MOmbudsman::class;
    // public static $globallySearchable = false;

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'Omb_name';
    public static function label()
    {
        return 'Омбудсман';
    }

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'Omb_name',
    ];

    /**
     * Get the fields displayed by the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function fields(Request $request)
    {
        $nav = DB::table('m_article')
            ->select('Ar_name as name', 'Ar_id as id')
            ->get()->pluck('name', 'id');


        return [
            ID::make(__('ID'), 'Omb_id')->sortable(),
            Text::make('Име', 'Omb_name')->sortable()->rules('required'),

            Date::make('Начало мандат', 'Omb_date_from')->rules('required')->sortable(),
            Date::make('Край мандат', 'Omb_date_to')->sortable(),
            Image::make('Снимка', 'Omb_photo')->disk('public')->path('pub/gallery')->storeAs(function (Request $request) {
                return date('YmdGis') . '_' . $request->file('Omb_photo')->getClientOriginalName();
            })->creationRules('required'),



            Select2::make('Вътрешна секция', 'Ar_id')
                ->sortable()
                // ->required()
                ->options($nav)
                ->displayUsingLabels()
                ->showAsLink()
                ->onlyOnForms()
                ->configuration([
                    'placeholder'             => __('Choose an option'),
                    'allowClear'              => true,
                    'minimumResultsForSearch' => 1,
                    'multiple'                => false,
                ]),

            HasMany::make('Езиковa версия', 'eq_lng', MOmbudsmanLng::class),



        ];
    }

    /**
     * Get the cards available for the request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function cards(Request $request)
    {
        return [];
    }

    /**
     * Get the filters available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function filters(Request $request)
    {
        return [];
    }

    /**
     * Get the lenses available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function lenses(Request $request)
    {
        return [];
    }

    /**
     * Get the actions available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function actions(Request $request)
    {
        return [];
    }
}
