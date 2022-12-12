<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int    $OmbL_id
 * @property int    $created_at
 * @property int    $deleted_at
 * @property int    $Omb_id
 * @property int    $S_Lng_id
 * @property int    $updated_at
 * @property string $OmbL_body
 * @property string $OmbL_intro
 * @property string $OmbL_title
 */
class MOmbudsmanLng extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_ombudsman_lng';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'OmbL_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'created_at', 'deleted_at', 'Omb_id', 'OmbL_body', 'OmbL_intro', 'OmbL_title', 'S_Lng_id', 'updated_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'OmbL_id' => 'int', 'created_at' => 'timestamp', 'deleted_at' => 'timestamp', 'Omb_id' => 'int', 'OmbL_body' => 'string', 'OmbL_intro' => 'string', 'OmbL_title' => 'string', 'S_Lng_id' => 'int', 'updated_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'deleted_at', 'updated_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();


        static::creating(function ($article) {

            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {

            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...
    public function eq_lng()
    {
        return $this->belongsTo(SLang::class, 'S_Lng_id');
    }
    public function eq_omb()
    {
        return $this->belongsTo(MOmbudsman::class, 'Omb_id');
    }
}
